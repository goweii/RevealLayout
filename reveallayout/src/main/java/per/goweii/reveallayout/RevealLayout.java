package per.goweii.reveallayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * 揭示效果布局
 * 可以指定2个子布局，以水波纹揭示效果切换选中状态展示
 *
 * @author Cuizhen
 * @date 2018/9/25
 */
public class RevealLayout extends FrameLayout {

    private View mCheckedView;
    private View mUncheckedView;

    private int mCheckedLayoutId;
    private int mUncheckedLayoutId;
    private boolean mChecked;
    private long mAnimDuration;
    private boolean mCheckWithExpand;
    private boolean mUncheckWithExpand;
    private boolean mAllowRevert;

    private float mCenterX;
    private float mCenterY;
    private float mRevealRadius = 0;
    private final Path mPath = new Path();
    private ValueAnimator mAnimator;

    public RevealLayout(Context context) {
        this(context, null);
    }

    public RevealLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(attrs);
        initView();
    }

    protected void initAttr(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.RevealLayout);
        mCheckedLayoutId = array.getResourceId(R.styleable.RevealLayout_rl_checked_layout, 0);
        mUncheckedLayoutId = array.getResourceId(R.styleable.RevealLayout_rl_unchecked_layout, 0);
        mChecked = array.getBoolean(R.styleable.RevealLayout_rl_checked, false);
        mAnimDuration = array.getInteger(R.styleable.RevealLayout_rl_anim_duration, 500);
        mCheckWithExpand = array.getBoolean(R.styleable.RevealLayout_rl_check_with_expand, true);
        mUncheckWithExpand = array.getBoolean(R.styleable.RevealLayout_rl_uncheck_with_expand, false);
        mAllowRevert = array.getBoolean(R.styleable.RevealLayout_rl_allow_revert, false);
        array.recycle();
    }

    private void initView() {
        mCheckedView = createCheckedView();
        mUncheckedView = createUncheckedView();
        addView(mCheckedView, getDefaultLayoutParams());
        addView(mUncheckedView, getDefaultLayoutParams());
        setChecked(mChecked);
    }

    protected View createCheckedView() {
        View checkedView;
        if (mCheckedLayoutId > 0) {
            checkedView = inflate(getContext(), mCheckedLayoutId, null);
        } else {
            checkedView = new View(getContext());
        }
        return checkedView;
    }

    protected View createUncheckedView() {
        View uncheckedView;
        if (mUncheckedLayoutId > 0) {
            uncheckedView = inflate(getContext(), mUncheckedLayoutId, null);
        } else {
            uncheckedView = new View(getContext());
        }
        return uncheckedView;
    }

    private LayoutParams getDefaultLayoutParams() {
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        return params;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                return true;
            default:
                break;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return isValidClick(event.getX(), event.getY());
            case MotionEvent.ACTION_UP:
                float unX = event.getX();
                float unY = event.getY();
                if (isValidClick(unX, unY)) {
                    if (mAnimator != null) {
                        if (mAllowRevert) {
                            toggle();
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        mRevealRadius = 0;
                        mCenterX = unX;
                        mCenterY = unY;
                        toggle();
                        return true;
                    }
                } else {
                    return false;
                }
            default:
                break;
        }
        return false;
    }

    private boolean isValidClick(float x, float y) {
        return x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight();
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        if (mChecked) {
            mCheckedView.bringToFront();
        } else {
            mUncheckedView.bringToFront();
        }
    }

    public void toggle() {
        mChecked = !mChecked;
        if (mAnimator != null) {
            mAnimator.reverse();
        } else {
            createRevealAnim();
        }
    }

    private void createRevealAnim() {
        float[] value = calculateAnimOfFloat();
        mRevealRadius = value[0];
        mAnimator = ValueAnimator.ofFloat(value[0], value[1]);
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.setDuration(mAnimDuration);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRevealRadius = (float) animation.getAnimatedValue();
                resetPath();
                invalidate();
            }
        });
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                resetPath();
                bringCurrentViewToFront();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimator = null;
                bringCurrentViewToFront();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mAnimator = null;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mAnimator.start();
    }

    private float[] calculateAnimOfFloat(){
        float fromValue;
        float toValue;
        float maxRadius = calculateMaxRadius();
        if (mChecked) {
            if (mCheckWithExpand) {
                fromValue = 0;
                toValue = maxRadius;
            } else {
                fromValue = maxRadius;
                toValue = 0;
            }
        } else {
            if (mUncheckWithExpand) {
                fromValue = 0;
                toValue = maxRadius;
            } else {
                fromValue = maxRadius;
                toValue = 0;
            }
        }
        return new float[]{fromValue, toValue};
    }

    private void resetPath() {
        mPath.reset();
        mPath.addCircle(mCenterX, mCenterY, mRevealRadius, Path.Direction.CW);
    }

    private void bringCurrentViewToFront() {
        if (mRevealRadius == 0) {
            if (mChecked) {
                mCheckedView.bringToFront();
            } else {
                mUncheckedView.bringToFront();
            }
        }
    }

    private float calculateMaxRadius() {
        float x = Math.max(mCenterX, getMeasuredWidth() - mCenterX);
        float y = Math.max(mCenterY, getMeasuredHeight() - mCenterY);
        return (float) Math.hypot(x, y);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (mAnimator == null) {
            return super.drawChild(canvas, child, drawingTime);
        }
        if (isBottomChild(child)) {
            return super.drawChild(canvas, child, drawingTime);
        }
        canvas.save();
        canvas.clipPath(mPath);
        boolean drawChild = super.drawChild(canvas, child, drawingTime);
        canvas.restore();
        return drawChild;
    }

    private boolean isBottomChild(View child) {
        return getChildAt(0) == child;
    }
}
