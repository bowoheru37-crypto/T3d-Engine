package com.t3d.sketchware;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.t3d.sketchware.engine.T3DEngineCore;
import com.t3d.sketchware.engine.T3DEntity;
import com.t3d.sketchware.engine.T3DMeshFactory;
import com.t3d.sketchware.engine.T3DSurfaceView;

/**
 * MainActivity - Sketchware Pro V7.0.0 Stable 3D Engine Template.
 * 100% Native Pure Java (No Lambdas, Zero Memory Leaks, Zero NPEs, Low Heat).
 */
public class MainActivity extends Activity {

    private T3DSurfaceView surfaceView;
    private TextView tvFps;
    private TextView tvEntities;
    private Button btnAddCube;
    private Button btnAddSphere;
    private Button btnAddTerrain;
    private Button btnEco;
    private Button btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initViews();
        setupEventListeners();
    }

    private void initViews() {
        surfaceView = (T3DSurfaceView) findViewById(R.id.t3d_surface_view);
        tvFps = (TextView) findViewById(R.id.tv_fps);
        tvEntities = (TextView) findViewById(R.id.tv_entities);
        btnAddCube = (Button) findViewById(R.id.btn_add_cube);
        btnAddSphere = (Button) findViewById(R.id.btn_add_sphere);
        btnAddTerrain = (Button) findViewById(R.id.btn_add_terrain);
        btnEco = (Button) findViewById(R.id.btn_eco);
        btnReset = (Button) findViewById(R.id.btn_reset);
    }

    private void setupEventListeners() {
        if (surfaceView != null) {
            // FPS and Performance Monitoring Callback
            surfaceView.setPerformanceListener(new T3DSurfaceView.PerformanceListener() {
                @Override
                public void onFpsUpdated(int fps, float frameTimeMs, int entityCount) {
                    if (tvFps != null) {
                        tvFps.setText("FPS: " + fps + " (" + String.format("%.1f", frameTimeMs) + "ms)");
                    }
                    if (tvEntities != null) {
                        tvEntities.setText("Entities: " + entityCount);
                    }
                }
            });
        }

        // Add 3D Cube Button
        if (btnAddCube != null) {
            btnAddCube.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (surfaceView != null) {
                        T3DEngineCore engine = surfaceView.getEngine();
                        if (engine != null) {
                            T3DEntity cube = new T3DEntity("Cube_" + (engine.entities.size() + 1));
                            cube.mesh = T3DMeshFactory.createCube(1.5f, Color.argb(230, 255, 159, 10));
                            cube.position.set(
                                    (float) ((Math.random() * 4.0) - 2.0),
                                    (float) ((Math.random() * 3.0) - 1.0),
                                    (float) ((Math.random() * 4.0) - 2.0)
                            );
                            engine.addEntity(cube);
                        }
                    }
                }
            });
        }

        // Add 3D Sphere Button
        if (btnAddSphere != null) {
            btnAddSphere.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (surfaceView != null) {
                        T3DEngineCore engine = surfaceView.getEngine();
                        if (engine != null) {
                            T3DEntity sphere = new T3DEntity("Sphere_" + (engine.entities.size() + 1));
                            sphere.mesh = T3DMeshFactory.createSphere(1.0f, 8, 8, Color.argb(230, 56, 189, 248));
                            sphere.position.set(
                                    (float) ((Math.random() * 4.0) - 2.0),
                                    (float) ((Math.random() * 3.0) - 1.0),
                                    (float) ((Math.random() * 4.0) - 2.0)
                            );
                            engine.addEntity(sphere);
                        }
                    }
                }
            });
        }

        // Add 3D Terrain / Pyramid Button
        if (btnAddTerrain != null) {
            btnAddTerrain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (surfaceView != null) {
                        T3DEngineCore engine = surfaceView.getEngine();
                        if (engine != null) {
                            T3DEntity pyramid = new T3DEntity("Pyramid_" + (engine.entities.size() + 1));
                            pyramid.mesh = T3DMeshFactory.createPyramid(2.0f, 2.5f, Color.argb(230, 244, 63, 94));
                            pyramid.position.set(
                                    (float) ((Math.random() * 4.0) - 2.0),
                                    (float) ((Math.random() * 2.0) - 1.0),
                                    (float) ((Math.random() * 4.0) - 2.0)
                            );
                            engine.addEntity(pyramid);
                        }
                    }
                }
            });
        }

        // Eco Mode (Toggle 60 FPS / 30 FPS for low heat and battery saving)
        if (btnEco != null) {
            btnEco.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (surfaceView != null) {
                        int currentFps = surfaceView.getTargetFps();
                        if (currentFps == 60) {
                            surfaceView.setTargetFps(30);
                            btnEco.setText("30 FPS (Eco)");
                            btnEco.setTextColor(Color.parseColor("#10B981"));
                        } else {
                            surfaceView.setTargetFps(60);
                            btnEco.setText("60 FPS");
                            btnEco.setTextColor(Color.parseColor("#00F0FF"));
                        }
                    }
                }
            });
        }

        // Reset Scene Button
        if (btnReset != null) {
            btnReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (surfaceView != null) {
                        T3DEngineCore engine = surfaceView.getEngine();
                        if (engine != null) {
                            engine.initDefaultScene();
                        }
                    }
                }
            });
        }
    }

    // =========================================================================
    // LIFECYCLE MANAGEMENT (Zero Memory Leaks & Zero Force-Close on Sleep)
    // =========================================================================
    @Override
    protected void onResume() {
        super.onResume();
        if (surfaceView != null) {
            surfaceView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (surfaceView != null) {
            surfaceView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (surfaceView != null) {
            surfaceView.stopRenderThread();
        }
        super.onDestroy();
    }
}
