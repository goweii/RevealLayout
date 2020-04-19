package per.goweii.reveallayout;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Checkable;
import android.widget.FrameLayout;

/**
 * 揭示效果布局
 * 可以指定2个子布局，以圆形揭示效果切换选中状态
 *
 * @author Cuizhen
 * @date 2018/9/25
 */
public class RevealLayout extends FrameLayout
        implements Checkable, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener, GestureDetector.OnGestureListener {

    private final GestureDetector mGestureDetector;

    private View mCheckedView;
    private View mUncheckedView;

    private int mCheckedLayoutId = 0;
    private int mUncheckedLayoutId = 0;
    private int mAnimDuration = 500;
    private boolean mCheckWithExpand = true;
    private boolean mUncheckWithExpand = false;
    private boolean mAllowRevert = false;
    private boolean mHideBackView = true;

    private boolean mChecked = false;

    private float mCenterX = 0F;
    private float mCenterY = 0F;
    private float mRevealRadius = 0F;
    private final Path mPath = new Path();
    private ValueAnimator mAnimator;
    private TimeInterpolator mInterpolator = null;

    private OnCheckedChangeListener mOnCheckedChangeListener = null;
    private OnAnimStateChangeListener mOnAnimStateChangeListener = null;

    public RevealLayout(Context context) {
        this(context, null);
    }

    public RevealLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mGestureDetector = new GestureDetector(context, this);
        initAttr(attrs);
        initView();
    }

    /**
     * 获取布局文件携带的属性，子类复写该方法，获取子类定义的属性。
     * 获取到子类属性后可以在{@link #createCheckedView()}和{@link #createUncheckedView()}中使用
     *
     * @param attrs AttributeSet
     */
    protected void initAttr(AttributeSet attrs) {
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.RevealLayout);
        mCheckedLayoutId = array.getResourceId(R.styleable.RevealLayout_rl_checkedLayout, 0);
        mUncheckedLayoutId = array.getResourceId(R.styleable.RevealLayout_rl_uncheckedLayout, 0);
        mChecked = array.getBoolean(R.styleable.RevealLayout_rl_checked, mChecked);
        mAnimDuration = array.getInteger(R.styleable.RevealLayout_rl_animDuration, mAnimDuration);
        mCheckWithExpand = array.getBoolean(R.styleable.RevealLayout_rl_checkWithExpand, mCheckWithExpand);
        mUncheckWithExpand = array.getBoolean(R.styleable.RevealLayout_rl_uncheckWithExpand, mUncheckWithExpand);
        mAllowRevert = array.getBoolean(R.styleable.RevealLayout_rl_allowRevert, mAllowRevert);
        mHideBackView = array.getBoolean(R.styleable.RevealLayout_rl_hideBackView, mHideBackView);
        array.recycle();
    }

    /**
     * 初始化选中和未选中状态的控件，并设置默认状态
     */
    protected void initView() {
        removeAllViews();
        if (mCheckedView == null) {
            mCheckedView = createCheckedView();
        }
        if (mUncheckedView == null) {
            mUncheckedView = createUncheckedView();
        }
        ViewGroup.LayoutParams checkParams = mCheckedView.getLayoutParams();
        if (checkParams == null) {
            checkParams = getDefaultLayoutParams();
        }
        ViewGroup.LayoutParams uncheckParams = mUncheckedView.getLayoutParams();
        if (uncheckParams == null) {
            uncheckParams = getDefaultLayoutParams();
        }
        addViewInLayout(mCheckedView, getChildCount(), checkParams);
        addViewInLayout(mUncheckedView, getChildCount(), uncheckParams);
        showTwoView();
        bringFrontView();
        hideBackView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            resetCenter();
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (mAnimator == null) {
            return super.drawChild(canvas, child, drawingTime);
        }
        if (isBackView(child)) {
            return super.drawChild(canvas, child, drawingTime);
        }
        canvas.save();
        canvas.clipPath(mPath);
        boolean drawChild = super.drawChild(canvas, child, drawingTime);
        canvas.restore();
        return drawChild;
    }

    private LayoutParams getDefaultLayoutParams() {
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        return params;
    }

    /**
     * 创建选中状态的控件，子类可复写该方法，初始化自己的控件
     *
     * @return 选中状态的控件
     */
    protected View createCheckedView() {
        View checkedView;
        if (getCheckedLayoutId() > 0) {
            checkedView = LayoutInflater.from(getContext()).inflate(getCheckedLayoutId(), this, false);
        } else {
            checkedView = new View(getContext());
        }
        return checkedView;
    }

    protected int getCheckedLayoutId(){
        return mCheckedLayoutId;
    }

    /**
     * 创建非选中状态的控件，子类可复写该方法，初始化自己的控件
     *
     * @return 非选中状态的控件
     */
    protected View createUncheckedView() {
        View uncheckedView;
        if (getUncheckedLayoutId() > 0) {
            uncheckedView = LayoutInflater.from(getContext()).inflate(getUncheckedLayoutId(), this, false);
        } else {
            uncheckedView = new View(getContext());
        }
        return uncheckedView;
    }

    protected int getUncheckedLayoutId() {
        return mUncheckedLayoutId;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return isValidClick(e.getX(), e.getY());
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        float upX = e.getX();
        float upY = e.getY();
        if (mAnimator != null) {
            if (mAllowRevert) {
                performClick();
                return true;
            } else {
                return false;
            }
        } else {
            mRevealRadius = 0;
            mCenterX = upX;
            mCenterY = upY;
            performClick();
            return true;
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean performClick() {
        toggle();
        return super.performClick();
    }

    /**
     * 判断触摸位置是否在view内部，是否是合法点击
     *
     * @param x 触摸点x坐标
     * @param y 触摸点y坐标
     * @return 点击是否合法
     */
    private boolean isValidClick(float x, float y) {
        return x >= 0 &&
                x <= getWidth()/* - getPaddingLeft() - getPaddingRight()*/ &&
                y >= 0 &&
                y <= getHeight()/* - getPaddingTop() - getPaddingBottom()*/;
    }

    /**
     * 创建揭示动画
     */
    private ValueAnimator createRevealAnim() {
        float[] value = calculateAnimOfFloat();
        mRevealRadius = value[0];
        ValueAnimator animator = ValueAnimator.ofFloat(value[0], value[1]);
        animator.setInterpolator(mInterpolator != null ? mInterpolator : new DecelerateInterpolator());
        animator.setDuration(mAnimDuration);
        animator.addUpdateListener(this);
        animator.addListener(this);
        return animator;
    }

    private void onCheckedChanged(boolean checked) {
        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this, checked);
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        mRevealRadius = (float) animation.getAnimatedValue();
        resetPath();
        invalidate();
    }

    @Override
    public void onAnimationStart(Animator animation) {
        resetPath();
        bringCurrentViewToFront();
        if (mOnAnimStateChangeListener != null) {
            mOnAnimStateChangeListener.onStart();
        }
    }

    public void onAnimationReverse() {
        if (mOnAnimStateChangeListener != null) {
            mOnAnimStateChangeListener.onReverse();
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mAnimator = null;
        bringCurrentViewToFront();
        hideBackView();
        resetCenter();
        if (mOnAnimStateChangeListener != null) {
            mOnAnimStateChangeListener.onEnd();
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }

    /**
     * 根据选中状态和揭示动画的扩散效果计算动画开始和结束半径
     *
     * @return {起始半径，结束半径}
     */
    private float[] calculateAnimOfFloat() {
        float fromValue;
        float toValue;
        float minRadius = calculateMinRadius();
        float maxRadius = calculateMaxRadius();
        if (mChecked) {
            if (mCheckWithExpand) {
                fromValue = minRadius;
                toValue = maxRadius;
            } else {
                fromValue = maxRadius;
                toValue = minRadius;
            }
        } else {
            if (mUncheckWithExpand) {
                fromValue = minRadius;
                toValue = maxRadius;
            } else {
                fromValue = maxRadius;
                toValue = minRadius;
            }
        }
        return new float[]{fromValue, toValue};
    }

    private void resetPath() {
        mPath.reset();
        mPath.addCircle(mCenterX, mCenterY, mRevealRadius, Path.Direction.CW);
    }

    /**
     * 将当前状态的view显示在顶部
     */
    private void bringCurrentViewToFront() {
        showTwoView();
        float minRadius = calculateMinRadius();
        float maxRadius = calculateMaxRadius();
        if (mRevealRadius < (minRadius + maxRadius) / 2F) {
            bringFrontView();
        }
    }

    private void bringFrontView() {
        if (mChecked) {
            mCheckedView.bringToFront();
        } else {
            mUncheckedView.bringToFront();
        }
    }

    private void showTwoView() {
        mCheckedView.setVisibility(VISIBLE);
        mUncheckedView.setVisibility(VISIBLE);
    }

    private void hideBackView() {
        if (!mHideBackView) {
            return;
        }
        if (mChecked) {
            mUncheckedView.setVisibility(INVISIBLE);
        } else {
            mCheckedView.setVisibility(INVISIBLE);
        }
    }

    /**
     * 计算揭示效果做大圆形半径，及圆心到4个边角的最大距离
     *
     * @return 最小半径
     */
    private float calculateMinRadius() {
        float w = getMeasuredWidth();
        float h = getMeasuredHeight();
        float l = getPaddingLeft();
        float t = getPaddingTop();
        float r = getPaddingRight();
        float b = getPaddingBottom();
        float x = Math.max(l - mCenterX, mCenterX - (w - r));
        float y = Math.max(t - mCenterY, mCenterY - (h - b));
        x = Math.max(x, 0);
        y = Math.max(y, 0);
        return (float) Math.hypot(x, y);
    }

    /**
     * 计算揭示效果做大圆形半径，及圆心到4个边角的最大距离
     *
     * @return 最大半径
     */
    private float calculateMaxRadius() {
        float w = getMeasuredWidth();
        float h = getMeasuredHeight();
        float l = getPaddingLeft();
        float t = getPaddingTop();
        float r = getPaddingRight();
        float b = getPaddingBottom();
        float x = Math.max(mCenterX - l, w - r - mCenterX);
        float y = Math.max(mCenterY - t, h - b - mCenterY);
        x = Math.max(x, 0);
        y = Math.max(y, 0);
        return (float) Math.hypot(x, y);
    }

    private boolean isBackView(View child) {
        return getChildAt(0) == child;
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        super.setOnClickListener(onClickListener);
    }

    /**
     * 设置选中状态改变的监听器
     *
     * @param onCheckedChangeListener OnCheckedChangeListener
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
        mOnCheckedChangeListener = onCheckedChangeListener;
    }

    /**
     * 设置动画状态改变的监听器
     *
     * @param onAnimStateChangeListener OnAnimStateChangeListener
     */
    public void setOnAnimStateChangeListener(OnAnimStateChangeListener onAnimStateChangeListener) {
        mOnAnimStateChangeListener = onAnimStateChangeListener;
    }

    /**
     * 获取当前选中状态
     *
     * @return 是否选中
     */
    @Override
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * 设置选中状态
     *
     * @param checked 是否选中
     */
    @Override
    public void setChecked(boolean checked) {
        if (mChecked == checked) return;
        mChecked = checked;
        onCheckedChanged(mChecked);
        if (mAnimDuration > 0) {
            if (mAnimator != null) {
                mAnimator.reverse();
                onAnimationReverse();
            } else {
                mAnimator = createRevealAnim();
                mAnimator.start();
            }
        } else {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }
            showTwoView();
            bringFrontView();
            hideBackView();
            resetCenter();
        }
    }

    /**
     * 设置选中状态
     *
     * @param checked  是否选中
     * @param withAnim 是否有动画
     */
    public void setChecked(boolean checked, boolean withAnim) {
        if (mChecked == checked) return;
        mChecked = checked;
        onCheckedChanged(mChecked);
        if (withAnim && mAnimDuration > 0) {
            if (mAnimator != null) {
                mAnimator.reverse();
                onAnimationReverse();
            } else {
                mAnimator = createRevealAnim();
                mAnimator.start();
            }
        } else {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }
            showTwoView();
            bringFrontView();
            hideBackView();
            resetCenter();
        }
    }

    /**
     * 切换选中状态，带有动画效果
     */
    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    public void resetCenter() {
        float w = getMeasuredWidth();
        float h = getMeasuredHeight();
        float l = getPaddingLeft();
        float t = getPaddingTop();
        float r = getPaddingRight();
        float b = getPaddingBottom();
        mCenterX = l + ((w - l - r) / 2F);
        mCenterY = t + ((h - t - b) / 2F);
    }

    public void setCenterPercent(float centerPercentX, float centerPercentY) {
        float centerX = getWidth() * centerPercentX;
        float centerY = getHeight() * centerPercentY;
        setCenter(centerX, centerY);
    }

    public void setCenter(float centerX, float centerY) {
        mCenterX = centerX;
        mCenterY = centerY;
    }

    public float getCenterX() {
        return mCenterX;
    }

    public float getCenterY() {
        return mCenterY;
    }

    public void setAllowRevert(boolean allowRevert) {
        mAllowRevert = allowRevert;
    }

    public void setAnimDuration(int animDuration) {
        mAnimDuration = animDuration;
    }

    public void setInterpolator(TimeInterpolator interpolator) {
        mInterpolator = interpolator;
    }

    public void setCheckWithExpand(boolean checkWithExpand) {
        mCheckWithExpand = checkWithExpand;
    }

    public void setUncheckWithExpand(boolean uncheckWithExpand) {
        mUncheckWithExpand = uncheckWithExpand;
    }

    public void setCheckedView(View checkedView) {
        if (checkedView == null) {
            return;
        }
        if (mCheckedView == checkedView) {
            return;
        }
        removeViewInLayout(mCheckedView);
        mCheckedView = checkedView;
        ViewGroup.LayoutParams checkParams = mCheckedView.getLayoutParams();
        if (checkParams == null) {
            checkParams = getDefaultLayoutParams();
        }
        addViewInLayout(mCheckedView, getChildCount(), checkParams);
        showTwoView();
        bringFrontView();
        hideBackView();
    }

    public void setUncheckedView(View uncheckedView) {
        if (uncheckedView == null) {
            return;
        }
        if (mUncheckedView == uncheckedView) {
            return;
        }
        removeViewInLayout(mUncheckedView);
        mUncheckedView = uncheckedView;
        ViewGroup.LayoutParams uncheckParams = mUncheckedView.getLayoutParams();
        if (uncheckParams == null) {
            uncheckParams = getDefaultLayoutParams();
        }
        addViewInLayout(mUncheckedView, getChildCount(), uncheckParams);
        showTwoView();
        bringFrontView();
        hideBackView();
    }

    public void setCheckedLayoutId(int checkedLayoutId) {
        mCheckedLayoutId = checkedLayoutId;
        setCheckedView(createCheckedView());
    }

    public void setUncheckedLayoutId(int uncheckedLayoutId) {
        mUncheckedLayoutId = uncheckedLayoutId;
        setUncheckedView(createUncheckedView());
    }

    public interface OnCheckedChangeListener {
        /**
         * 选中状态改变
         *
         * @param revealLayout RevealLayout
         * @param isChecked    当前选中状态
         */
        void onCheckedChanged(RevealLayout revealLayout, boolean isChecked);
    }

    public interface OnAnimStateChangeListener {
        /**
         * 动画开始时调用
         */
        void onStart();

        /**
         * 动画回滚时调用。即上一个动画尚未结束时切换选中状态，动画回滚到之前状态
         */
        void onReverse();

        /**
         * 动画结束时调用
         */
        void onEnd();
    }
}
