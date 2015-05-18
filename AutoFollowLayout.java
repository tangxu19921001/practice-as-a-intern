package com.melot.meshow.room;

import com.melot.meshow.R;
import com.melot.meshow.Setting;
import com.melot.meshow.main.onActivityStateListener;
import com.melot.meshow.room.ChatRoom.flagState;
import com.melot.meshow.sns.ActivityTaskManager;
import com.melot.meshow.sns.HttpManager;
import com.melot.meshow.struct.ServerTask;
import com.melot.meshow.util.Log;
import com.melot.meshow.widget.CustomDialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

/**
 * AutoFollowLayout
 *
 * @author TangXu
 */
public class AutoFollowLayout extends RelativeLayout implements
        onActivityStateListener {
    public final String TAG = AutoFollowLayout.class.getSimpleName();

    /** 关注人数限制，超过该数，则不提示关注和订阅 */
    public static final int AUTO_FOLLOWS_TIP_COUNT = 5;

    /** handler处理事件 */
    private final int ANIM_DELAY = 800;

    private final int ANIM_FINISH_DELAY = 6250;

    private final int ANIM_START_DELAY = 60000;

    private final int ANIM_START = 0x00001;

    private final int ANIM_FINISH = 0x00002;

    private View mView;
    private Context mContext;
    private CustomDialog.Builder mCustomDialog;

    private Animation showAnim_in;
    private Animation showAnim_out;

    private ActivityTaskManager taskManager;

    private long mRoomId;

    private boolean isShow;

    private int actorTAG;

    public AutoFollowLayout(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public AutoFollowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public AutoFollowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case ANIM_START:
                    startAnimation();
                    break;
                case ANIM_FINISH:
                    finishAnimation();
                    break;
            }
        }
    };

    private void init() {
        mView = LayoutInflater.from(getContext()).inflate(
                R.layout.kk_auto_follow_layout, null);
        addView(mView);
        setVisibility(View.GONE);

        taskManager = new ActivityTaskManager();

        loadAnimation();

        setOnClickListener(mTipClickListener);
    }

    private OnClickListener mTipClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!(getContext() instanceof ChatRoom)) {
                return;
            }
            flagState addState = ((ChatRoom) getContext()).getFlagState();
            Log.v("addState", String.valueOf(addState));

            ServerTask followedTask = HttpManager.getInstance().followFriend(
                    mRoomId);
            taskManager.addTask(followedTask);

            if (mHandler != null)
                mHandler.removeMessages(ANIM_FINISH);

            if (addState == flagState.NOADD) {
                setVisibility(View.GONE);
                if (actorTAG == 0) {
                    return;
                }
                mCustomDialog = new CustomDialog.Builder(mContext);
                mCustomDialog
                        .setTitle(R.string.kk_follow_success)
                        .setMessage(R.string.kk_auto_followed_tip)
                        .setPositiveButton(R.string.kk_room_flag_notice,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                        ServerTask addFlagTask = HttpManager
                                                .getInstance().addRoomFlag(
                                                        mRoomId);
                                        taskManager.addTask(addFlagTask);

                                    }
                                })
                        .setNegativeButton(R.string.kk_room_flag_cancel, null);
                mCustomDialog.setCancelable(true);
                mCustomDialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });

                mCustomDialog.create().show();

            } else if (addState == flagState.HASADD) {
                setVisibility(View.GONE);
            }

        }
    };

    /**
     * 如果用户关注数小于3，用户每次在一个房间停留2分钟，弹框提示“K宝发现您已经默默停留了2分钟，关注TA下次就不会错过了”“取消”“关注”，
     * 关注对应的应为是关注和订阅
     *
     * @param mRoomId    b
     * @param isFollowed c
     */
    public void startAutoFollow(long mRoomId,
                                boolean isFollowed, int actorTAG) {
        this.mRoomId = mRoomId;
        this.actorTAG = actorTAG;

        if (isShow || isFollowed || mRoomId == Setting.getInstance().getUserId()) {
            return;
        }

        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.what = ANIM_START;
            mHandler.sendMessageDelayed(msg, ANIM_START_DELAY);
            Log.v(TAG, "startAutoFollow" + msg);
        }

    }

    public void loadAnimation() {
        Log.v(TAG, "loadAnimation");
        showAnim_in = AnimationUtils.loadAnimation(getContext(),
                R.anim.kk_fade_in);
        showAnim_in.setDuration(ANIM_DELAY);
        showAnim_out = AnimationUtils.loadAnimation(getContext(),
                R.anim.kk_fade_out);
    }

    public void startAnimation() {
        Log.v(TAG, "startAnimation");

        if (Setting.getInstance().hasInFollows(mRoomId)) {
            return;
        }
        setVisibility(View.VISIBLE);
        startAnimation(showAnim_in);
        isShow = true;

        Message msg = mHandler.obtainMessage();
        msg.what = ANIM_FINISH;
        mHandler.sendMessageDelayed(msg, ANIM_FINISH_DELAY);
    }

    private void finishAnimation() {
        if (showAnim_out == null)
            return;
        startAnimation(showAnim_out);
        showAnim_out.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
            }
        });
        Log.v(TAG, "finishAnimation");
    }

    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean isShow) {
        this.isShow = isShow;
        removeTask();
    }

    public void removeTask(){
        if (mHandler != null) {
            mHandler.removeMessages(ANIM_START);
        }
    }

    @Override
    public void onActivityResume() {

    }

    @Override
    public void onActivityPaused() {

    }

    @Override
    public void onActivityDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

}