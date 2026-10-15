package com.t3d.sketchware.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * T3DSurfaceView - High-Performance, Zero-Leak SurfaceView Game Loop for Sketchware Pro.
 * Compatible with Sketchware Pro V7.0.0 Stable (No Lambdas, Zero Memory Leaks, Low Heat).
 */
public class T3DSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface PerformanceListener {
        void onFpsUpdated(int fps, float frameTimeMs, int entityCount);
    }

    private final T3DEngineCore engine;
    private SurfaceHolder surfaceHolder;
    private Thread renderThread;
    private volatile boolean isRunning;
    private volatile boolean isPaused;

    // Frame Rate Target & Thermal Throttling Protection
    private int targetFps = 60;
    private long framePeriodMs = 1000 / 60;

    // Touch Orbit & Zoom State
    private float lastTouchX;
    private float lastTouchY;
    private float lastPinchDist;
    private int activePointerCount = 0;

    // Performance Stats
    private PerformanceListener performanceListener;
    private long lastFpsTime = 0;
    private int frameCount = 0;

    public T3DSurfaceView(Context context) {
        super(context);
        this.engine = new T3DEngineCore();
        init();
    }

    public T3DSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.engine = new T3DEngineCore();
        init();
    }

    private void init() {
        this.surfaceHolder = getHolder();
        if (surfaceHolder != null) {
            surfaceHolder.addCallback(this);
        }
        setFocusable(true);
    }

    public T3DEngineCore getEngine() {
        return engine;
    }

    public void setPerformanceListener(PerformanceListener listener) {
        this.performanceListener = listener;
    }

    public void setTargetFps(int fps) {
        this.targetFps = (fps > 0) ? fps : 60;
        this.framePeriodMs = 1000 / this.targetFps;
    }

    public int getTargetFps() {
        return targetFps;
    }

    // =========================================================================
    // SURFACE LIFECYCLE
    // =========================================================================
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startRenderThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Handled automatically on next draw
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopRenderThread();
    }

    public synchronized void startRenderThread() {
        if (renderThread != null && renderThread.isAlive()) {
            return;
        }
        isRunning = true;
        isPaused = false;
        renderThread = new Thread(this, "T3D-RenderThread");
        renderThread.start();
    }

    public synchronized void stopRenderThread() {
        isRunning = false;
        if (renderThread != null) {
            try {
                renderThread.join(500);
            } catch (InterruptedException ignored) {
            }
            renderThread = null;
        }
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    // =========================================================================
    // MAIN ZERO-GC GAME LOOP (Anti-Overheating Precision Ticker)
    // =========================================================================
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        lastFpsTime = System.currentTimeMillis();
        frameCount = 0;

        while (isRunning) {
            if (isPaused) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }

            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1000000000.0f;
            lastTime = now;

            // Cap delta time to prevent physics explosions on pause resume
            if (deltaTime > 0.1f) deltaTime = 0.1f;

            // 1. Update Simulation
            engine.update(deltaTime);

            // 2. Render Frame
            Canvas canvas = null;
            try {
                if (surfaceHolder != null) {
                    canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        int w = getWidth();
                        int h = getHeight();
                        if (w > 0 && h > 0) {
                            engine.render(canvas, w, h);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Defensive catch against background surface discard
            } finally {
                if (canvas != null && surfaceHolder != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            // 3. FPS Monitoring & Listener Notification
            frameCount++;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFpsTime >= 1000) {
                final int currentFps = frameCount;
                final float avgFrameTime = (1000.0f / Math.max(1, currentFps));
                final int entityCount = engine.entities.size();

                frameCount = 0;
                lastFpsTime = currentTime;

                if (performanceListener != null) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (performanceListener != null) {
                                performanceListener.onFpsUpdated(currentFps, avgFrameTime, entityCount);
                            }
                        }
                    });
                }
            }

            // 4. Precision Sleep (Thermal Throttling Prevention)
            long frameDurationMs = (System.nanoTime() - now) / 1000000;
            long sleepTime = framePeriodMs - frameDurationMs;
            if (sleepTime > 1) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    // =========================================================================
    // TOUCH GESTURES (Single Touch: Orbit, Multi-Touch: Zoom)
    // =========================================================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;

        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX(0);
                lastTouchY = event.getY(0);
                activePointerCount = 1;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount >= 2) {
                    activePointerCount = 2;
                    lastPinchDist = calculateDistance(event);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (activePointerCount == 1 && pointerCount == 1) {
                    float dx = event.getX(0) - lastTouchX;
                    float dy = event.getY(0) - lastTouchY;

                    engine.cameraYaw += dx * 0.4f;
                    engine.cameraPitch += dy * 0.4f;

                    // Clamp Pitch
                    if (engine.cameraPitch > 85.0f) engine.cameraPitch = 85.0f;
                    if (engine.cameraPitch < -85.0f) engine.cameraPitch = -85.0f;

                    lastTouchX = event.getX(0);
                    lastTouchY = event.getY(0);
                } else if (pointerCount >= 2) {
                    float currentDist = calculateDistance(event);
                    float diff = currentDist - lastPinchDist;

                    engine.cameraDistance -= diff * 0.02f;
                    if (engine.cameraDistance < 2.0f) engine.cameraDistance = 2.0f;
                    if (engine.cameraDistance > 30.0f) engine.cameraDistance = 30.0f;

                    lastPinchDist = currentDist;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerCount = 0;
                return true;
        }

        return super.onTouchEvent(event);
    }

    private float calculateDistance(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0f;
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt((x * x) + (y * y));
    }
}
