package com.shine.niceapp.ui.fragment;

import android.animation.Animator;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.handmark.pulltorefresh.extras.viewpager.PullToRefreshViewPager;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.shine.niceapp.R;
import com.shine.niceapp.bean.Card;
import com.shine.niceapp.control.IRhythmItemListener;
import com.shine.niceapp.control.RhythmAdapter;
import com.shine.niceapp.control.RhythmLayout;
import com.shine.niceapp.control.ViewPagerScroller;
import com.shine.niceapp.ui.adapter.CardPagerAdapter;
import com.shine.niceapp.ui.widget.ProgressHUD;
import com.shine.niceapp.utils.AnimatorUtils;
import com.shine.niceapp.utils.HexUtils;
import com.shine.niceapp.utils.NetWorkHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * User: shine
 * Date: 2014-12-13
 * Time: 19:45
 * Description:
 */
public class CardViewPagerFragment extends AbsBaseFragment implements PullToRefreshBase.OnRefreshListener<ViewPager> {

    private TextView mTimeFirstText;
    private TextView mTimeSecondText;

    /**
     * 最外层的View，为了设置背景颜色而使用
     */
    private View mMainView;
    private ImageButton mRocketToHeadBtn;
    private Button mSideMenuOrBackBtn;
    /**
     * 钢琴布局
     */
    private RhythmLayout mRhythmLayout;
    /**
     * 可以侧拉刷新的ViewPager，其实是一个LinearLayout控件
     */
    private PullToRefreshViewPager mPullToRefreshViewPager;
    /**
     * 接收PullToRefreshViewPager中的ViewPager控件
     */
    private ViewPager mViewPager;
    /**
     * ViewPager的适配器
     */
    private CardPagerAdapter mCardPagerAdapter;
    /**
     * 记录上一个选项卡的颜色值
     */
    private int mPreColor;

    private boolean mHasNext = true;

    private boolean mIsRequesting;

    private boolean isAdapterUpdated;

    private int mCurrentViewPagerPage;


    private List<Card> mCardList;

    private ProgressHUD mProgressHUD;

    /**
     * 钢琴布局的适配器
     */
    private RhythmAdapter mRhythmAdapter;

    private static CardViewPagerFragment mFragment;

    /**
     * 自定义钢琴控件的监听器
     */
    private IRhythmItemListener rhythmItemListener = new IRhythmItemListener() {
        public void onRhythmItemChanged(int paramInt) {
        }

        public void onSelected(final int paramInt) {
            CardViewPagerFragment.this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    mViewPager.setCurrentItem(paramInt);
                }
            }, 100L);
        }

        public void onStartSwipe() {
        }
    };
    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        public void onPageScrollStateChanged(int paramInt) {
        }

        public void onPageScrolled(int paramInt1, float paramFloat, int paramInt2) {
        }

        public void onPageSelected(int position) {
            onAppPagerChange(position);
            if (mHasNext && (position > -10 + mCardList.size()) && !mIsRequesting && NetWorkHelper.isWifiDataEnable(getActivity())) {
                fetchData();
            }
        }
    };

    public static CardViewPagerFragment getInstance() {
        if (mFragment == null) {
            mFragment = new CardViewPagerFragment();
        }
        return mFragment;
    }


    @Override
    protected View initViews(LayoutInflater inflater) {
        //初始化控件
        View view = inflater.inflate(R.layout.fragment_niceapp, null);
        mTimeFirstText = (TextView) view.findViewById(R.id.text_time_first);
        mTimeSecondText = (TextView) view.findViewById(R.id.text_time_second);
        mMainView = view.findViewById(R.id.main_view);
        mRocketToHeadBtn = (ImageButton) view.findViewById(R.id.btn_rocket_to_head);
        mSideMenuOrBackBtn = (Button) view.findViewById(R.id.btn_side_menu_or_back);
        mRhythmLayout = (RhythmLayout) view.findViewById(R.id.box_rhythm);
        mPullToRefreshViewPager = (PullToRefreshViewPager) view.findViewById(R.id.pager);
        mViewPager = mPullToRefreshViewPager.getRefreshableView();
        //设置ViewPager的滚动速度
        setViewPagerScrollSpeed(mViewPager, 400);
        //设置ScrollView滚动动画延迟执行的时间
        mRhythmLayout.setScrollRhythmStartDelayTime(400);
        //设置钢琴布局的高度 高度为钢琴布局item的宽度+10dp
        int height = (int) mRhythmLayout.getRhythmItemWidth() + (int) TypedValue.applyDimension(1, 10.0F, getResources().getDisplayMetrics());
        mRhythmLayout.getLayoutParams().height = height;
        ((RelativeLayout.LayoutParams) mPullToRefreshViewPager.getLayoutParams()).bottomMargin = height;

        mTimeSecondText.setText("12月\n星期六");

        return view;
    }


    @Override
    protected void initActions(View paramView) {
        //设置控件的监听
        mRhythmLayout.setRhythmListener(rhythmItemListener);
        mPullToRefreshViewPager.setOnRefreshListener(this);
        mViewPager.setOnPageChangeListener(onPageChangeListener);
        mRocketToHeadBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View paramView) {
                CardViewPagerFragment.this.mViewPager.setCurrentItem(0, true);
            }
        });

    }

    @Override
    protected void initData() {
        mCardList = new ArrayList<>();
    }

    /**
     * 设置ViewPager的滚动速度，即每个选项卡的切换速度
     *
     * @param viewPager ViewPager控件
     * @param speed     滚动速度，毫秒为单位
     */
    private void setViewPagerScrollSpeed(ViewPager viewPager, int speed) {
        try {
            Field field = ViewPager.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            ViewPagerScroller viewPagerScroller = new ViewPagerScroller(viewPager.getContext(), new OvershootInterpolator(0.6F));
            field.set(viewPager, viewPagerScroller);
            viewPagerScroller.setDuration(speed);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 改变当前选中钢琴按钮
     *
     * @param position viewPager的位置
     */
    private void onAppPagerChange(int position) {
        //执行动画，改变升起的钢琴按钮
        mRhythmLayout.showRhythmAtPosition(position);
        toggleRocketBtn(position);
        Card post = this.mCardList.get(position);
        //得到当前的背景颜色
        int currColor = HexUtils.getHexColor(post.getBackgroundColor());
        //执行颜色转换动画
        AnimatorUtils.showBackgroundColorAnimation(this.mMainView, mPreColor, currColor, 400);
        mPreColor = currColor;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fetchData();
        onAppPagerChange(0);
    }

    /**
     * 加载数据
     */
    private void fetchData() {
        ArrayList<Card> cardList = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            int m = i % 8;
            Card card = addData(m);
            cardList.add(card);
        }
        mPreColor = HexUtils.getHexColor(cardList.get(0).getBackgroundColor());
        updateAppAdapter(cardList);
    }


    private void updateAppAdapter(List<Card> cardList) {
        if ((getActivity() == null) || (getActivity().isFinishing())) {
            return;
        }
        if (mProgressHUD != null && mProgressHUD.isShowing()) {
            this.mProgressHUD.dismiss();
            this.isAdapterUpdated = true;
        }
        if (cardList.isEmpty()) {
            this.mMainView.setBackgroundColor(this.mPreColor);
            return;
        }
        int size = mCardList.size();

        if (mCardPagerAdapter == null) {
            mCurrentViewPagerPage = 0;
            mCardPagerAdapter = new CardPagerAdapter(getActivity().getSupportFragmentManager(), cardList);
            mViewPager.setAdapter(mCardPagerAdapter);
        } else {
            mCardPagerAdapter.addCardList(cardList);
            mCardPagerAdapter.notifyDataSetChanged();
        }
        addCardIconsToDock(cardList);

        this.mCardList = mCardPagerAdapter.getCardList();

        if (mViewPager.getCurrentItem() == size - 1)
            mViewPager.setCurrentItem(1 + mViewPager.getCurrentItem(), true);
    }

    private void addCardIconsToDock(final List<Card> cardList) {
        if (mRhythmAdapter == null) {
            resetRhythmLayout(cardList);
            return;
        }
        mRhythmAdapter.addCardList(cardList);
        mRhythmAdapter.notifyDataSetChanged();
    }

    //重置钢琴控件数据源
    private void resetRhythmLayout(List<Card> cardList) {
        if (getActivity() == null)
            return;
        if (cardList == null)
            cardList = new ArrayList<>();
        mRhythmAdapter = new RhythmAdapter(getActivity(), mRhythmLayout, cardList);
        mRhythmLayout.setAdapter(mRhythmAdapter);
    }

    /**
     * viewPager刷新或加载更多监听
     *
     * @param pullToRefreshBase
     */
    public void onRefresh(PullToRefreshBase<ViewPager> pullToRefreshBase) {
        if (this.mIsRequesting)
            return;
        if (pullToRefreshBase.getCurrentMode() == PullToRefreshBase.Mode.PULL_FROM_END) {//最右
            mIsRequesting = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fetchData();
                    mPullToRefreshViewPager.onRefreshComplete();
                    mIsRequesting = false;
                }
            }, 2000);

        } else if (pullToRefreshBase.getCurrentMode() == PullToRefreshBase.Mode.PULL_FROM_START) {//最左
            mIsRequesting = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPullToRefreshViewPager.onRefreshComplete();
                    mIsRequesting = false;
                }
            }, 2000);
        }
    }

    /**
     * 根据当前viewPager的位置决定右上方的火箭图案是否显示
     *
     * @param position
     */
    private void toggleRocketBtn(int position) {
        if (position > 1) {
            if (mRocketToHeadBtn.getVisibility() == View.GONE) {
                mRocketToHeadBtn.setVisibility(View.VISIBLE);
                AnimatorUtils.animViewFadeIn(this.mRocketToHeadBtn);
            }
        } else if (this.mRocketToHeadBtn.getVisibility() == View.VISIBLE) {
            AnimatorUtils.animViewFadeOut(this.mRocketToHeadBtn).addListener(new Animator.AnimatorListener() {
                public void onAnimationCancel(Animator paramAnimator) {
                }

                public void onAnimationEnd(Animator paramAnimator) {
                    CardViewPagerFragment.this.mRocketToHeadBtn.setVisibility(View.GONE);
                }

                public void onAnimationRepeat(Animator paramAnimator) {
                }

                public void onAnimationStart(Animator paramAnimator) {
                }
            });
        }
        mTimeFirstText.setText((position + 1) + "");
    }


    private Card addData(int i) {
        Card card = new Card();
        switch (i) {
            case 0:
                card.setTitle("God of Light");
                card.setSubTitle("点亮世界之光");
                card.setDigest("当下制造精致的游戏往往超越了常规概念中对游戏的界定。通关的过程更像是在欣赏一部电影大片。通过镜片反射，即使只有一道光芒，我们也能点亮世界");
                card.setUpNum(124);
                card.setAuthorName("小美");
                card.setBackgroundColor("#00aac6");
                card.setCoverImgerUrl("card_cover1");
                card.setIconUrl("card_icon1");
                break;
            case 1:
                card.setTitle("我的手机与众不同");
                card.setSubTitle("专题");
                card.setDigest("谁说美化一定要Root?选对了应用一样可以美美哒～有个性，爱折腾，我们不爱啃苹果，我们是大安卓用户!都说「世界上没有相同的叶子」，想让自己的手机与众不同?让小美告诉你");
                card.setUpNum(299);
                card.setAuthorName("小美");
                card.setBackgroundColor("#dc4e97");
                card.setCoverImgerUrl("card_cover2");
                card.setIconUrl("card_icon2");
                break;
            case 2:
                card.setTitle("BlackLight");
                card.setSubTitle("做最纯粹的微博客户端");
                card.setDigest("Android的官方微博客户端显得太过臃肿，这让不少人转而投向第三方客户端。「Fuubo」、「四次元」、「Smooth」，一个个耳熟能详的名字，它们各有千秋，也吸引了一大票追随者，而今天推荐的BlackLight，又是一个被重复造出的「轮子」，然而这个后来者可不一般");
                card.setUpNum(241);
                card.setAuthorName("小最");
                card.setBackgroundColor("#00aac6");
                card.setCoverImgerUrl("card_cover3");
                card.setIconUrl("card_icon3");
                break;
            case 3:
                card.setTitle("BuzzFeed");
                card.setSubTitle("最好玩的新闻在这里");
                card.setDigest("BuzzFeed是一款聚合新闻阅读应用，这款应用来自美国用户增长流量最快，内容最能吸引大众眼球的互联网新闻网站，当然我们不必知道BuzzFeed的创始人多么流弊，BuzzFeed本身是多么具有颠覆性，我们只需要知道这款应用的内容绝对有料，而去也是十分精致，简洁");
                card.setUpNum(119);
                card.setAuthorName("小最");
                card.setBackgroundColor("#e76153");
                card.setCoverImgerUrl("card_cover4");
                card.setIconUrl("card_icon4");
                break;
            case 4:
                card.setTitle("Nester");
                card.setSubTitle("专治各种熊孩子");
                card.setDigest("Nester简单的说是一款用于家长限制孩子玩手机的应用，这只可爱的圆滚滚的小鸟不仅可以设置孩子可以使用的应用，还可以用定时器控释孩子玩手机的时长。在小最看来，Nester最直白的描述就是专治各种熊孩子");
                card.setUpNum(97);
                card.setAuthorName("小最");
                card.setBackgroundColor("#9a6dbb");
                card.setCoverImgerUrl("card_cover5");
                card.setIconUrl("card_icon5");
                break;
            case 5:
                card.setTitle("二次元专题");
                card.setSubTitle("啊喂，别总想去四维空间啦");
                card.setDigest("为了满足美友中不少二次元少年的需求，小最前几日特(bei)意(po)被拍扁为二维状，去那个神奇的世界走了一遭。在被深深震撼之后，为大家带来本次「二次元专题」");
                card.setUpNum(317);
                card.setAuthorName("小最");
                card.setBackgroundColor("#51aa53");
                card.setCoverImgerUrl("card_cover6");
                card.setIconUrl("card_icon6");
                break;
            case 6:
                card.setTitle("Music Player");
                card.setSubTitle("闻其名，余音绕梁");
                card.setDigest("一款App，纯粹到极致，便是回到原点「Music Player」，一款音乐播放器，一个干净到显得敷衍的名字。它所打动的，是哪些需要音乐，才可以慰借心灵的人。");
                card.setUpNum(385);
                card.setAuthorName("小最");
                card.setBackgroundColor("#ea5272");
                card.setCoverImgerUrl("card_cover7");
                card.setIconUrl("card_icon7");
                break;
            case 7:
                card.setTitle("el");
                card.setSubTitle("剪纸人の唯美旅程");
                card.setDigest("断崖之上，孤牢中醒来的他，意外地得到一把能乘风翱翔的伞，于是在悠扬的钢琴曲中，剪纸人开始了漫无目的的漂泊之旅。脚下的重峦叠嶂，飞行中遇到的种种障碍，不日又遇到了他，将会有一段怎样的旅程?");
                card.setUpNum(622);
                card.setAuthorName("小美");
                card.setBackgroundColor("#e76153");
                card.setCoverImgerUrl("card_cover8");
                card.setIconUrl("card_icon8");
                break;
        }
        return card;
    }
}
