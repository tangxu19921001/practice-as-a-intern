package com.melot.meshow.room;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.melot.meshow.R;
import com.melot.meshow.main.onActivityStateListener;
import com.melot.meshow.struct.Active;

public class SongAdapter extends BaseAdapter implements onActivityStateListener {

	@SuppressWarnings("unused")
	private static final String TAG = SongAdapter.class.getSimpleName();

	private Context mContext;
	private ArrayList<Active> songList;
	private int mCount;
	private ChoiceSongClickListener mChoiceListener;

	public SongAdapter(ChoiceSong con) {
		mContext = con;
		songList = new ArrayList<Active>();
	}

	public interface ChoiceSongClickListener {
		void onclick(String songId);
	}

	public void setOnClickListener(ChoiceSongClickListener l) {
		mChoiceListener = l;
	}

	@Override
	public int getCount() {
		return mCount;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		SongItem item = null;
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.kk_choice_song_item, null);
			item = new SongItem();
			item.choiceButton = (Button) convertView.findViewById(R.id.song_operate);
			item.lastView = (TextView) convertView.findViewById(R.id.last_view);
			item.singer = (TextView) convertView.findViewById(R.id.song_singer);
			item.songName = (TextView) convertView.findViewById(R.id.song_name);
			convertView.setTag(item);
		} else {
			item = (SongItem) convertView.getTag();
		}

		Active node = songList.get(position);
		if (node != null) {
			if (!TextUtils.isEmpty(node.getActiveOwner()) && !node.getActiveOwner().equals("null")) {
				item.singer.setText(node.getActiveOwner());
				item.singer.setVisibility(View.VISIBLE);
			} else {
				item.singer.setVisibility(View.GONE);
			}
			if (!TextUtils.isEmpty(node.getActiveName()) && !node.getActiveName().equals("null")) {
				item.songName.setText(node.getActiveName());
				item.songName.setVisibility(View.VISIBLE);
			} else {
				item.songName.setVisibility(View.GONE);
			}
			if (!TextUtils.isEmpty(node.getActiveId()) && !node.getActiveId().equals("null")) {
				item.choiceButton.setTag(node.getActiveId());
				item.choiceButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						mChoiceListener.onclick(v.getTag().toString());
					}
				});
			}
		}
		if (position == mCount - 1 && songList.size() > 6) {
			item.lastView.setVisibility(View.VISIBLE);
		} else {
			item.lastView.setVisibility(View.GONE);
		}
		return convertView;
	}

	class SongItem {
		TextView songName;
		TextView singer;
		TextView lastView;
		Button choiceButton;
	}

	@Override
	public void onActivityResume() {
		notifyDataSetChanged();
	}

	@Override
	public void onActivityPaused() {

	}

	@Override
	public void onActivityDestroy() {
		mContext = null;
		mCount = 0;
		if (songList != null) {
			songList.clear();
		}
	}

	public void setSongList(List<Active> list) {
		if (list == null)
			throw new NullPointerException("can not add null songList");
		if (songList != null)
			songList.clear();
		songList.addAll(list);
		mCount = songList.size();
		notifyDataSetChanged();
	}

}
