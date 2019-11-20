package de.dpd.vanassist.controls;

import android.animation.*;
import android.content.Context;
import android.graphics.drawable.*;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import de.dpd.vanassist.R;


/* Custom view for Sliding Button (Confirm or Decline parcel delivery) */
public class SwipeButton extends RelativeLayout {
    private OnSwipeButtonListener listener;

    public interface OnSwipeButtonListener {
        void onSwipeButtonMoved(View v);
        void OnSwipeButtonConfirm(View v);
        void OnSwipeButtonDecline(View v);
        void OnSwipeButtonFaded(View v);
    }

    public void setSwipeListener(OnSwipeButtonListener listener) {
        this.listener = listener;
    }

    private ImageView slidingButton;
    private float initialX;
    private boolean active;
    private TextView leftArrowsText;
    private TextView rightArrowsText;
    private float downX, upX;

    private Drawable disabledDrawable;
    private Drawable enabledDrawable;

    private boolean enabled;

    public SwipeButton(Context context) {
        super(context);
        init(context);
    }

    public SwipeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /* Initialize SwipeButton */
    private void init(Context context) {
        /*  add background of slider */
        RelativeLayout background = new RelativeLayout(context);

        LayoutParams layoutParamsView = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParamsView.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        background.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_rounded));

        addView(background, layoutParamsView);

        /* left arrows */
        final TextView leftArrowsText = new TextView(context);
        this.leftArrowsText = leftArrowsText;
        leftArrowsText.setGravity(Gravity.CENTER);

        LayoutParams lLayoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        leftArrowsText.setText("<<<");
        leftArrowsText.setTextColor(getResources().getColor(R.color.logoRed));
        leftArrowsText.setTextSize(25);
        background.addView(leftArrowsText, lLayoutParams);

        /* right arrows */
        final TextView rightArrowsText = new TextView(context);
        this.rightArrowsText = rightArrowsText;
        rightArrowsText.setGravity(Gravity.CENTER);

        LayoutParams rLayoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        rightArrowsText.setText(">>>");
        rightArrowsText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        rightArrowsText.setTextSize(25);
        background.addView(rightArrowsText, rLayoutParams);

        /* add the moving icon */
        ImageView swipeButton = new ImageView(context);
        slidingButton = swipeButton;

        disabledDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_dpd_box_black);
        enabledDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_dpd_box_black);

        slidingButton.setImageDrawable(disabledDrawable);
        slidingButton.setPadding(40, 40, 40, 40);

        LayoutParams layoutParamsButton = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        layoutParamsButton.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        layoutParamsButton.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        layoutParamsButton.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        swipeButton.setBackground(ContextCompat.getDrawable(context, R.drawable.shape_button));
        swipeButton.setImageDrawable(enabledDrawable);
        addView(swipeButton, layoutParamsButton);

        slidingButton.post( new Runnable() {
            @Override
            public void run() {
                initialX = (getMeasuredWidth() - slidingButton.getMeasuredWidth()) / 2;
                slidingButton.setX(initialX);
                leftArrowsText.setPadding((int)initialX/2, 25, 0, 25);
                rightArrowsText.setPadding(0, 25, (int)initialX/2, 25);
            }
        });

        /* set Touch listener */
        setOnTouchListener(getButtonTouchListener());
    }


    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    /* Touch listener for swipe button interaction
     * @return TouchListener
     */
    private OnTouchListener getButtonTouchListener() {
        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        /* button follows the finger and textView is disappearing (opacity -> 0) */
                        if (event.getX() > slidingButton.getWidth() / 2 &&
                                event.getX() + slidingButton.getWidth() / 2 < getWidth()) {
                            slidingButton.setX(event.getX() - slidingButton.getWidth() / 2);

                            /* sliding left */
                            if (slidingButton.getX() < initialX) {
                                float opacityPercentage = ((initialX - slidingButton.getX()) / initialX);
                                slidingButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.shape_button_swiping_left));
                                slidingButton.getBackground().setAlpha((int)(opacityPercentage*255));
                                leftArrowsText.setAlpha((float) (1 - 1.3 * opacityPercentage));
                                rightArrowsText.setAlpha((float) (1 - 1.3 * opacityPercentage));
                            }
                            /* sliding right */
                            else if (slidingButton.getX() > initialX) {
                                float opacityPercentage = ((- initialX + slidingButton.getX()) / initialX);
                                slidingButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.shape_button_swiping_right));
                                slidingButton.getBackground().setAlpha((int)(opacityPercentage*255));
                                leftArrowsText.setAlpha((float) (1 - 1.3 * opacityPercentage));
                                rightArrowsText.setAlpha((float) (1 - 1.3 * opacityPercentage));
                            }
                            else {
                                slidingButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.shape_button));
                                slidingButton.getBackground().setAlpha(1);
                                leftArrowsText.setAlpha(1);
                                rightArrowsText.setAlpha(1);
                            }

                        }
                        /* set button to the limit of component when swiping outside the limits */
                        if  (event.getX() + slidingButton.getWidth() / 2 > getWidth() &&
                                slidingButton.getX() + slidingButton.getWidth() / 2 < getWidth()) {
                            slidingButton.setX(getWidth() - slidingButton.getWidth());
                        }

                        if  (event.getX() < slidingButton.getWidth() / 2) {
                            slidingButton.setX(0);
                        }
                        if (listener != null)
                            listener.onSwipeButtonMoved(SwipeButton.this);
                        return true;

                    case MotionEvent.ACTION_UP:
                        upX = event.getX();
                        float deltaX = downX - upX;
                        if(Math.abs(deltaX) > initialX){
                            if(deltaX < 0)
                            {
                                /* left to right == confirm */
                                expandButton(true);
                                fadeSlider();
                                return true;
                            }
                            if(deltaX > 0) {
                                /* right to left == decline */
                                expandButton(false);
                                fadeSlider();
                                return true;
                            }
                        }
                        else {
                            /* not long enough swipe... */
                            moveButtonBack();
                            return false;
                        }
                        return true;
                }
                return false;
            }
        };
    }


    /* Animation for fading swipeButton */
    private void fadeSlider() {
        AlphaAnimation animation1 = new AlphaAnimation(1f, 0f);
        animation1.setDuration(1000);
        animation1.setFillAfter(false);
        animation1.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) { }
            @Override
            public void onAnimationRepeat(Animation arg0) { }
            @Override
            public void onAnimationEnd(Animation arg0) {
                /* reset button */
                removeAllViews();
                init(getContext());

                /* raise event */
                if (listener != null) {
                    listener.OnSwipeButtonFaded(SwipeButton.this);
                }
            }
        });
        slidingButton.startAnimation(animation1);
    }


    /* Animation for move back */
    private void moveButtonBack() {
        final ValueAnimator positionAnimator =
                ValueAnimator.ofFloat(slidingButton.getX(), initialX);
        positionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        positionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float x = (Float) positionAnimator.getAnimatedValue();
                slidingButton.setX(x);
            }
        });

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(
                leftArrowsText, "alpha", 1);
        ObjectAnimator objectAnimator2 = ObjectAnimator.ofFloat(
                rightArrowsText, "alpha", 1);

        positionAnimator.setDuration(200);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimator, objectAnimator2, positionAnimator);
        animatorSet.start();

        slidingButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.shape_button));
    }


    /* Animation for expand */
    private void expandButton(final boolean confirmed) {
        final ValueAnimator positionAnimator =
                ValueAnimator.ofFloat(slidingButton.getX(), 0);
        positionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float x = (Float) positionAnimator.getAnimatedValue();
                slidingButton.setX(x);
            }
        });

        final ValueAnimator widthAnimator = ValueAnimator.ofInt(
                slidingButton.getWidth(),
                getWidth());

        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams params = slidingButton.getLayoutParams();
                params.width = (Integer) widthAnimator.getAnimatedValue();
                slidingButton.setLayoutParams(params);
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                active = true;
                slidingButton.setImageDrawable(enabledDrawable);
            }
        });

        animatorSet.playTogether(positionAnimator, widthAnimator);
        animatorSet.start();

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                /* raise event */
                if (listener != null) {
                    if (confirmed)
                        listener.OnSwipeButtonConfirm(SwipeButton.this);
                    else
                        listener.OnSwipeButtonDecline(SwipeButton.this);
                }
            }
        });
    }


    /* Animation for collapse */
    private void collapseButton() {
        final ValueAnimator widthAnimator = ValueAnimator.ofInt(
                slidingButton.getWidth(), ((int) initialX));

        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams params =  slidingButton.getLayoutParams();
                params.width = (Integer) widthAnimator.getAnimatedValue();
                slidingButton.setLayoutParams(params);
            }
        });

        widthAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                active = false;
                slidingButton.setImageDrawable(disabledDrawable);
            }
        });

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(
                leftArrowsText, "alpha", 1);

        AnimatorSet animatorSet = new AnimatorSet();

        animatorSet.playTogether(objectAnimator, widthAnimator);
        animatorSet.start();
    }
}
