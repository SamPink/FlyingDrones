package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import java.util.Random;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 *
 * @author normenhansen
 */
public class Main extends SimpleApplication implements ActionListener {

    private static AppSettings initSettings() {
        AppSettings cfg = new AppSettings(true);
        cfg.setFrameRate(60); // set to less than or equal screen refresh rate
        cfg.setVSync(true);   // prevents page tearing
        cfg.setFrequency(60); // set to screen refresh rate
        cfg.setResolution(1024, 768);   
        cfg.setFullscreen(false); 
        cfg.setSamples(2);    // anti-aliasing
        cfg.setTitle("My jMonkeyEngine 3 Game"); // branding: window name
        // branding: load splashscreen from assets
        cfg.setSettingsDialogImage("Interface/MySplashscreen.png");
        return cfg;
    }


    private Spatial sceneModel;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private Player player;
    private final Vector3f walkDirection = new Vector3f();
    private boolean left = false, right = false, up = false, down = false;
    private Geometry mark;
    private Node ships;

    //Temporary vectors used on each frame.
    //They here to avoid instanciating new vectors on each frame
    private final Vector3f camDir = new Vector3f();
    private final Vector3f camLeft = new Vector3f();

    public static void main(String[] args) {   
        Main app = new Main();
        
        app.setSettings(initSettings());
        
        app.setShowSettings(false);
       
        app.start();
    }

    @Override
    public void simpleInitApp() {

        initMark();
        initCrossHairs();
        /**
         * Set up Physics
         */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(true);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);
        setUpKeys();
        setUpLight();

        // We load the scene from the zip file and adjust its size.
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);
        //sceneModel = assetManager.loadModel("Scenes/leve1.j3o");
        //rootNode.attachChild(sceneModel);

        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        player = new Player();

        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(sceneModel);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().add(player.getPlayer());

        // You can change the gravity of individual physics objects before or after
        //they are added to the PhysicsSpace, but it must be set before MOVING the
        //physics location.
        player.getPlayer().setGravity(new Vector3f(0, 0, 0));
        player.getPlayer().setPhysicsLocation(new Vector3f(0, 50, 0));

        ships = new Node("Ships");

        rootNode.attachChild(ships);

        createShip();
    }

    public void createShip() {

        Material defaultMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Vector3f location = cam.getLocation();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                Random r = new Random();
                
                Spatial teapot = assetManager.loadModel("Models/Duck/Duck.j3o");
                teapot.setMaterial(defaultMat);
                teapot.setLocalTranslation(
                        (r.nextInt(50)),
                        (50),
                        (i*j*3)
                );
                teapot.scale(5);

                CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(teapot);
                RigidBodyControl thing = new RigidBodyControl(sceneShape, 0);

                teapot.addControl(thing);

                bulletAppState.getPhysicsSpace().add(thing);

                ships.attachChild(teapot);
            }
        }
    }

    private void setUpLight() {
        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);
    }

    /**
     * A centred plus sign to help the player aim.
     */
    protected void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - ch.getLineWidth() / 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    protected void initMark() {
        Sphere sphere = new Sphere(30, 30, 0.2f);
        mark = new Geometry("BOOM!", sphere);
        Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mark_mat.setColor("Color", ColorRGBA.Red);
        mark.setMaterial(mark_mat);
    }

    /**
     * We over-write some navigational key mappings here, so we can add
     * physics-controlled walking and jumping:
     */
    private void setUpKeys() {
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");
        inputManager.addListener(this, "Jump");
    }

    /**
     * * These are our custom actions triggered by key presses.We do not walk
     * yet, we just keep track of the direction the user pressed.
     *
     * @param binding
     * @param isPressed
     * @param tpf
     */
    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding) {
            case "Left":
                left = isPressed;
                break;
            case "Right":
                right = isPressed;
                break;
            case "Up":
                up = isPressed;
                break;
            case "Down":
                down = isPressed;
                break;
            case "Jump":
                if (isPressed) {
                    //player.getPlayer().jump(new Vector3f(0, 20f, 0));
                    shoot();
                }
                break;
            default:
                break;
        }
    }

    public void shoot() {
        // 1. Reset results list.
        CollisionResults results = new CollisionResults();
        // 2. Aim the ray from cam loc to cam direction.
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        // 3. Collect intersections between Ray and Shootables in results list.
        // DO NOT check collision with the root node, or else ALL collisions will hit the
        // skybox! Always make a separate node for objects you want to collide with.
        ships.collideWith(ray, results);
        // 4. Print the results
        System.out.println("----- Collisions? " + results.size() + "-----");
        if (results.size() > 0) {

            // The closest collision point is what was truly hit:
            CollisionResult closest = results.getClosestCollision();
            // Let's interact - we mark the hit with a red dot.
            mark.setLocalTranslation(closest.getContactPoint());
            rootNode.attachChild(mark);
            closest.getGeometry().removeFromParent();

        } else {
            // No hits? Then remove the red mark.
            rootNode.detachChild(mark);
        }
    }

    /**
     * This is the main event loop--walking happens here.We check in which
     * direction the player is walking by interpreting the camera direction
     * forward (camDir) and to the side (camLeft). The setWalkDirection()
     * command is what lets a physics-controlled player walk. We also make sure
     * here that the camera moves with player.
     *
     * @param tpf
     */
    @Override
    public void simpleUpdate(float tpf) {

        camDir.set(cam.getDirection()).multLocal(1.5f); //move at 2x the camera speed in curent direction
        camLeft.set(cam.getLeft()).multLocal(0.4f);

        walkDirection.set(0, 0, 0);

        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }

        walkDirection.addLocal(camDir);
        player.getPlayer().setWalkDirection(walkDirection);
        cam.setLocation(player.getPlayer().getPhysicsLocation());
    }
}
