
package com.bitknights.locationalarm.info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bitknights.locationalarm.R;
import com.bitknights.locationalarm.BaseFragment;
import com.bitknights.locationalarm.LaunchActivity;

public class AboutFragment extends BaseFragment {

    public static AboutFragment instantiate(LaunchActivity activity, int position) {
        AboutFragment aboutFragment = new AboutFragment();
        return aboutFragment;
    }

    public static String getFragmentTag() {
        return AboutFragment.class.getName();
    }

    private Button mVisitWebpageButton;
    private Button mSendEmailButton;

    private Button mFollowButton;
    private Button mTweetButton;
    private Button mLikeButton;
    private Button mShareButton;

    @Override
    protected View getContentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.info, null);

        this.mVisitWebpageButton = (Button) contentView.findViewById(R.about.btnWebpageButton);
        this.mSendEmailButton = (Button) contentView.findViewById(R.about.btnEmail);

        this.mFollowButton = (Button) contentView.findViewById(R.about.btnFollowButton);
        this.mTweetButton = (Button) contentView.findViewById(R.about.btnTweetButton);
        this.mLikeButton = (Button) contentView.findViewById(R.about.btnLikeButton);
        this.mShareButton = (Button) contentView.findViewById(R.about.btnShareButton);

        this.mVisitWebpageButton.setOnClickListener(null);
        this.mSendEmailButton.setOnClickListener(null);

        this.mFollowButton.setOnClickListener(null);
        this.mTweetButton.setOnClickListener(null);
        this.mLikeButton.setOnClickListener(null);
        this.mShareButton.setOnClickListener(null);

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitleText(R.string.aboutTitle);
    }

    @Override
    public void onDestroyView() {
        this.mVisitWebpageButton.setOnClickListener(null);
        this.mSendEmailButton.setOnClickListener(null);

        this.mFollowButton.setOnClickListener(null);
        this.mTweetButton.setOnClickListener(null);
        this.mLikeButton.setOnClickListener(null);
        this.mShareButton.setOnClickListener(null);

        this.mVisitWebpageButton = null;
        this.mSendEmailButton = null;

        this.mFollowButton = null;
        this.mTweetButton = null;
        this.mLikeButton = null;
        this.mShareButton = null;

        super.onDestroyView();
    }

}
