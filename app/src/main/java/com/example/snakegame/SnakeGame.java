package com.example.snakegame;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;

class SnakeGame extends SurfaceView implements Runnable{

    // Objects for the game loop/thread
    private Thread mThread = null;
    // Control pausing between updates
    private long mNextFrameTime;
    // Is the game currently playing and or paused?
    private volatile boolean mPlaying = false;
    private volatile boolean mPaused = true;

    // for playing sound effects
    private SoundPool mSP;
    private int mEat_ID = -1;
    private int mCrashID = -1;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int mNumBlocksHigh;

    // How many points does the player have
    private int mScore;

    // Objects for drawing
    private Canvas mCanvas;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;

    // A snake ssss
    private Snake mSnake;
    // And an apple
    private Apple mApple;

    private Bitmap pauseImage;
    private Bitmap resumeImage;


    // This is the constructor method that gets called
    // from SnakeActivity
    public SnakeGame(Context context, Point size) {
        super(context);

        pauseImage = BitmapFactory.decodeResource(getResources(), R.drawable.pausebutton);
        resumeImage = BitmapFactory.decodeResource(getResources(), R.drawable.playbutton);

        // Work out how many pixels each block is
        int blockSize = size.x / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        mNumBlocksHigh = size.y / blockSize;

        // Initialize the SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSP = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            mSP = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }
        try {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Prepare the sounds in memory
            descriptor = assetManager.openFd("get_apple.ogg");
            mEat_ID = mSP.load(descriptor, 0);

            descriptor = assetManager.openFd("snake_death.ogg");
            mCrashID = mSP.load(descriptor, 0);

        } catch (IOException e) {
            // Error
        }

        // Initialize the drawing objects
        mSurfaceHolder = getHolder();
        mPaint = new Paint();

        // Call the constructors of our two game objects
        mApple = new Apple(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);

        mSnake = new Snake(context,
                new Point(NUM_BLOCKS_WIDE,
                        mNumBlocksHigh),
                blockSize);

    }


    // Called to start a new game
    public void newGame() {

        // reset the snake
        mSnake.reset(NUM_BLOCKS_WIDE, mNumBlocksHigh);

        // Get the apple ready for dinner
        mApple.spawn();

        // Reset the mScore
        mScore = 0;

        // Setup mNextFrameTime so an update can triggered
        mNextFrameTime = System.currentTimeMillis();
    }


    // Handles the game loop
    @Override
    public void run() {
        while (mPlaying) {
            if(!mPaused) {
                // Update 10 times a second
                if (updateRequired()) {
                    update();
                }
            }

            draw();
        }
    }


    // Check to see if it is time for an update
    public boolean updateRequired() {

        // Run at 10 frames per second
        final long TARGET_FPS = 10;
        // There are 1000 milliseconds in a second
        final long MILLIS_PER_SECOND = 1000;

        // Are we due to update the frame
        if(mNextFrameTime <= System.currentTimeMillis()){
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            mNextFrameTime =System.currentTimeMillis()
                    + MILLIS_PER_SECOND / TARGET_FPS;

            // Return true so that the update and draw
            // methods are executed
            return true;
        }

        return false;
    }


    // Update all the game objects
    public void update() {

        // Move the snake
        mSnake.move();

        // Did the head of the snake eat the apple?
        if(mSnake.checkDinner(mApple.getLocation())){
            // This reminds me of Edge of Tomorrow.
            // One day the apple will be ready!
            mApple.spawn();

            // Add to  mScore
            mScore = mScore + 1;

            // Play a sound
            mSP.play(mEat_ID, 1, 1, 0, 0, 1);
        }

        // Did the snake die?
        if (mSnake.detectDeath()) {
            // Pause the game ready to start again
            mSP.play(mCrashID, 1, 1, 0, 0, 1);

            mPaused =true;
        }

    }


    // Do all the drawing
    public void draw() {
        // Get a lock on the mCanvas
        if (mSurfaceHolder.getSurface().isValid()) {
            mCanvas = mSurfaceHolder.lockCanvas();

            // Fill the screen with a color
            mCanvas.drawColor(Color.argb(255, 100, 140, 70));

            // Set the size and color of the mPaint for the text
            mPaint.setColor(Color.argb(255, 255, 255, 255));
            mPaint.setTextSize(120);

            // Set a custom font
            Typeface customTypeface = Typeface.createFromAsset(getContext().getAssets(), "ShiftyNotesRegular-BWZ6d.ttf");
            mPaint.setTypeface(null);

            // Draw the score
            mCanvas.drawText("" + mScore, 20, 120, mPaint);


            // Draw the apple and the snake
            mApple.draw(mCanvas, mPaint);
            mSnake.draw(mCanvas, mPaint);

            // Dimensions of the screen
            int screenWidth = 1080;
            int screenHeight = 2220;

            int screenWidthText =  mCanvas.getWidth();;
            int screenHeightText =  mCanvas.getHeight();;

            // Switch between the two pause and resume images
            Bitmap buttonImage = (mPaused) ? resumeImage : pauseImage;

            // Get width and height of the images
            int buttonImageWidth = pauseImage.getWidth();
            int buttonImageHeight = resumeImage.getHeight();

            int x = screenWidth - buttonImageWidth;
            int y = 0;

            // Draw the image on the screen
            mCanvas.drawBitmap(buttonImage, x, y, null);

            // Draw names on the bottom right corner
            mPaint.setTypeface(customTypeface);
            String name1 = "Fadi Zubeideh";
            String name2 = "Ganesh Renukunta";
            float textWidth1 = mPaint.measureText(name1);
            float textWidth2 = mPaint.measureText(name2);
            float textHeight = mPaint.getFontMetrics().bottom - mPaint.getFontMetrics().top;
            mCanvas.drawText(name1, screenWidthText - textWidth1 - 20, screenHeightText - textHeight - 100, mPaint);
            mCanvas.drawText(name2, screenWidthText - textWidth2 - 20, screenHeightText - textHeight - 20, mPaint);

            // Draw some text while paused
            if(mPaused){

                // Set the size and color of the mPaint for the text
                mPaint.setColor(Color.argb(255, 255, 255, 255));
                mPaint.setTextSize(120);

                // Draw the message
                // We will give this an international upgrade soon
                mCanvas.drawText("Tap To Play!", 200, 700, mPaint);
                mCanvas.drawText(getResources().
                                getString(R.string.tap_to_play),
                        200, 700, mPaint);
            }

            // Unlock the mCanvas and reveal the graphics for this frame
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();

        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (isButtonClicked(touchX, touchY)) {
                // Toggle the game state between paused and resumed
                mPaused = !mPaused;

                if (mPaused) {
                    // If the game is paused, stop the snake movement
                    pauseGame();
                }
                return true;
            }
        }

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (mPaused) {
                    mPaused = false;
                    newGame();

                    // Don't want to process snake direction for this tap
                    return true;
                }

                // Let the Snake class handle the input
                mSnake.switchHeading(motionEvent);

            default:
                break;

        }
        return true;

    }

    private boolean isButtonClicked(float touchX, float touchY) {
        int screenWidth = 1080;
        int screenHeight = 2220;
        int buttonWidth = pauseImage.getWidth();
        int buttonHeight = resumeImage.getHeight();

        // Define the bounds of where the button can be clicked
        int buttonLeft = screenWidth - buttonWidth;
        int buttonTop = 0;
        int buttonRight = buttonLeft + buttonWidth ;
        int buttonBottom = buttonTop + buttonHeight;

        // Check if the touch coordinates are within the button bounds
        return touchX >= buttonLeft && touchX <= buttonRight && touchY >= buttonTop && touchY <= buttonBottom;
    }


    // Stop the thread
    public void pause() {
        mPlaying = false;
        try {
            mThread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }


    // Start the thread
    public void resume() {
        mPlaying = true;
        mThread = new Thread(this);
        mThread.start();
    }

    public void pauseGame() {
        mPaused = true;
    }

    public void resumeGame() {
        mPaused = false;
    }

    public boolean isPaused() {
        return mPaused;
    }

}