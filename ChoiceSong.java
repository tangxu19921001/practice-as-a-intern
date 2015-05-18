package com.melot.meshow.room;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.melot.meshow.Global;
import com.melot.meshow.KKLive;
import com.melot.meshow.R;
import com.melot.meshow.Setting;
import com.melot.meshow.room.SongAdapter.ChoiceSongClickListener;
import com.melot.meshow.sns.TaskType;
import com.melot.meshow.sns.socket.SocketMessagFormer;
import com.melot.meshow.struct.Active;
import com.melot.meshow.struct.Task;
import com.melot.meshow.util.AppMessage;
import com.melot.meshow.util.FirstPaymentWindow;
import com.melot.meshow.util.IMsgCallback;
import com.melot.meshow.util.Log;
import com.melot.meshow.util.MessageDump;
import com.melot.meshow.util.Util;
import com.melot.meshow.widget.CustomDialog;

public class ChoiceSong extends Activity implements IMsgCallback {

	public final static String ROOM_LV = "com.melot.meshow.room.ChoiceSong.roomLv";
	public final static String ROOM_ID = "com.melot.meshow.room.ChoiceSong.roomId";
	private final static String TAG = ChoiceSong.class.getSimpleName();
	private final static int UI_UPDATE_LIST = 0x000001;
	private final static int UI_SHOW_NOTIFY = 0x000002;

	private static final int STAR_LV_MONEY = 5000;
	private static final int DIAMOND_LV_MONEY = 10000;
	private static final int OTHER_LV_MONEY = 15000;

	private static List<Active> songList;
	private static Handler mChoiceHandler;

	private TextView mInfoTextView;
	private ProgressBar mProgress;
	private TextView mErrorTxt;
	private ListView mListView;
	private SongAdapter mAdapter;

	private static boolean isGetSongs = false;

	private int miRoomLv = 0;
	private long miRoomId = 0;
	private String mCallBack;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.kk_choice_song);

		if (!Setting.isInitialized()) {
			KKLive.init(ChoiceSong.this);
		}

		mCallBack = MessageDump.getInstance().RegistryCallback(this);

		inits();
		mChoiceHandler = new Handler() {
			public void dispatchMessage(android.os.Message msg) {
				switch (msg.what) {
				case UI_UPDATE_LIST:
					Log.i(TAG, "UI_UPDATE_LISTsetSongsData");
					setSongsData();
					break;
				case UI_SHOW_NOTIFY:
					if (mProgress != null) {
						mProgress.setVisibility(View.GONE);
						mListView.setVisibility(View.GONE);
						mErrorTxt.setVisibility(View.VISIBLE);
						mErrorTxt.setText(msg.arg1);
					}
					break;
				default:
					Log.e(TAG, "undefine msg type");
					break;
				}
			};
		};

		if (isGetSongs) {
			if (mChoiceHandler != null) {
				Message uiMsg = mChoiceHandler.obtainMessage(UI_UPDATE_LIST);
				uiMsg.arg1 = R.string.kk_no_data;
				if (!mChoiceHandler.hasMessages(uiMsg.what)) {
					mChoiceHandler.sendMessage(uiMsg);
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (!isFinishing()) {
			AppMessage msg = new AppMessage(TaskType.DISCONNECT_VIDEO, 0, 0, null, null, null);
			MessageDump.getInstance().dispatch(msg);
		}
	}

	protected void setSongsData() {
		if (mListView != null) {
			if (songList == null || songList.size() == 0) {
				Message msg = mChoiceHandler.obtainMessage(UI_SHOW_NOTIFY);
				msg.arg1 = R.string.kk_room_song_none;
				if (mChoiceHandler != null)
					mChoiceHandler.sendMessage(msg);
			} else {
				mAdapter.setSongList(songList);
				mListView.setVisibility(View.VISIBLE);
				mProgress.setVisibility(View.GONE);
				mErrorTxt.setVisibility(View.GONE);
			}
		}

	}

	private boolean needShowFirstFillMoney(Activity activity) {
		if (Setting.getInstance().getShowFirstFillMoneyView() == Task.TASK_STATE_NONE) {
			if (!isFinishing()) {
				FirstPaymentWindow window = new FirstPaymentWindow(ChoiceSong.this,
						FirstPaymentWindow.FILL_MONEY_NO_MONEY, miRoomId);
				window.create();
			}
			return true;
		}
		return false;
	}

	private void inits() {
		// title设置
		TextView title = (TextView) findViewById(R.id.kk_title_text);
		title.setText(R.string.kk_room_choice_song);
		// 左边btn
		ImageView backImage = (ImageView) findViewById(R.id.left_bt);
		backImage.setImageResource(R.drawable.kk_title_back);
		backImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		// 右边隐藏
		findViewById(R.id.right_bt).setVisibility(View.INVISIBLE);
		miRoomLv = getIntent().getIntExtra(ROOM_LV, 0);
		miRoomId = getIntent().getLongExtra(ROOM_ID, 0);
		mListView = (ListView) findViewById(R.id.song_list);
		mListView.setVisibility(View.GONE);
		mAdapter = new SongAdapter(this);
		mListView.setAdapter(mAdapter);
		mAdapter.setOnClickListener(new ChoiceSongClickListener() {
			@Override
			public void onclick(final String songId) {

				if (miRoomId == Setting.getInstance().getUserId()) {
					Util.showToast(ChoiceSong.this, R.string.kk_choice_song_myself);
					return;
				}

				long request = STAR_LV_MONEY;
				if (miRoomLv <= 6) {
					request = STAR_LV_MONEY;
				} else if (miRoomLv <= 11) {
					request = DIAMOND_LV_MONEY;
				} else {
					request = OTHER_LV_MONEY;
				}
				if (Setting.getInstance().getMoney() < request) {

					if (needShowFirstFillMoney(ChoiceSong.this))
						return;

					CustomDialog.Builder b = new CustomDialog.Builder(ChoiceSong.this);
					b.setTitle(getString(R.string.app_name))
							.setMessage(getString(R.string.kk_not_enough_money))
							.setPositiveButton(R.string.kk_give_money,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											Intent intent;
											try {
												intent = new Intent(
														ChoiceSong.this,
														Class.forName("com.melot.meshow.fillmoney.PaymentMethods"));
												intent.putExtra(Global.ROOM_ID, miRoomId);
												startActivity(intent);
												// wxc live
											} catch (ClassNotFoundException e) {
												e.printStackTrace();
												return;
											}
										}
									}).setNegativeButton(R.string.kk_cancel, null);
					b.setCancelable(false);
					b.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
						}
					});
					b.create().show();

				} else {
					CustomDialog.Builder b = new CustomDialog.Builder(ChoiceSong.this);
					b.setTitle(getString(R.string.app_name))
							.setMessage(
									getString(R.string.kk_song_choice_conform) + request
											+ getString(R.string.kk_money))
							.setPositiveButton(R.string.kk_ok,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											if (ChatRoom.instance != null) {
												String msg = SocketMessagFormer
														.createChoiceSongsMsg(songId);
												((ChatRoom) ChatRoom.instance).getMsgManger()
														.sendMessage(msg);
											}
										}
									})
							.setNegativeButton(R.string.kk_cancel,
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {

										}
									});
					b.setCancelable(false);
					b.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
						}
					});
					b.create().show();
				}

			}
		});
		mProgress = (ProgressBar) findViewById(R.id.progress);
		mErrorTxt = (TextView) findViewById(R.id.error_info);
		mErrorTxt.setText(getString(R.string.kk_loading));
		mInfoTextView = (TextView) findViewById(R.id.song_info_pre);
		String color = getResources().getString(R.string.kk_room_choose_song_price_color);
		String info = getString(R.string.kk_song_info_one) + "<br />"
				+ getString(R.string.kk_song_info_two) + "<font color=\'" + color + "\'>"
				+ getString(R.string.kk_song_info_three, OTHER_LV_MONEY) + "</font>"
				+ getString(R.string.kk_song_info_four) + "<font color=\'" + color + "\'>"
				+ getString(R.string.kk_song_info_five, DIAMOND_LV_MONEY) + "</font>"
				+ getString(R.string.kk_song_info_six) + "<font color=\'" + color + "\'>"
				+ getString(R.string.kk_song_info_last_money, STAR_LV_MONEY) + "</font>" + "<br />"
				+ getString(R.string.kk_song_info_last);
		mInfoTextView.setText(Html.fromHtml(info));
	}

	public static void setSongList(ArrayList<Active> list) {
		isGetSongs = true;
		if (songList != null) {
			songList.clear();
		}
		songList = list;
		if (mChoiceHandler != null) {
			Message uiMsg = mChoiceHandler.obtainMessage(UI_UPDATE_LIST);
			uiMsg.arg1 = R.string.kk_no_data;
			if (!mChoiceHandler.hasMessages(uiMsg.what)) {
				mChoiceHandler.sendMessage(uiMsg);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mAdapter != null) {
			mAdapter.onActivityResume();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			mAdapter.onActivityDestroy();
		}
		if (songList != null) {
			songList.clear();
		}
		MessageDump.getInstance().UnRegistryCallback(mCallBack);
		mCallBack = null;
		if (mChoiceHandler != null) {
			if (mChoiceHandler.hasMessages(UI_UPDATE_LIST))
				mChoiceHandler.removeMessages(UI_UPDATE_LIST);
			if (mChoiceHandler.hasMessages(UI_SHOW_NOTIFY))
				mChoiceHandler.removeMessages(UI_SHOW_NOTIFY);
		}
		isGetSongs = false;
	}

	// /**
	// * 当socket error 时调用
	// *
	// * @param errorId
	// */
	// public static void onSocketError(int errorId) {
	// if (mChoiceHandler != null) {
	// Message msg = mChoiceHandler.obtainMessage(UI_SHOW_NOTIFY);
	// msg.arg1 = errorId;
	// mChoiceHandler.sendMessage(msg);
	// }
	// }

	@Override
	public void onMsg(AppMessage msg) {
		if (msg.getMessageType() == TaskType.HTTP_FILL_MONEY
				|| msg.getMessageType() == TaskType.HTTP_REFRESH_MONEY) {
			if (msg.getRc() == 0 && msg.getLParam() > 0) {

				if (Setting.getInstance().getShowFirstFillMoneyView() != Task.TASK_STATE_FINISHED) {
					Setting.getInstance().setShowFirstFillMoneyView(Task.TASK_STATE_PENDDING);
					if (!isFinishing()) {
						FirstPaymentWindow window = new FirstPaymentWindow(ChoiceSong.this,
								Task.TASK_STATE_PENDDING);
						window.create();
					}
				}

			}
		}
	}
}
