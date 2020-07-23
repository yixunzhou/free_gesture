package team.tsinghua.ipsc.free_gesture.ui.main;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import team.tsinghua.ipsc.free_gesture.mode.SlideLRFragment;
import team.tsinghua.ipsc.free_gesture.mode.ZoomInFragment;
import team.tsinghua.ipsc.free_gesture.R;
import team.tsinghua.ipsc.free_gesture.mode.SlideUDFragment;
import team.tsinghua.ipsc.free_gesture.mode.TouchFragment;
import team.tsinghua.ipsc.free_gesture.mode.ZoomOutFragment;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    @StringRes
    private static final int[] TAB_TITLES = new int[]{R.string.tab_text_1, R.string.tab_text_12,
                            R.string.tab_text_2, R.string.tab_text_22, R.string.tab_text_3};
    private final Context mContext;
    private final int numOfMode = 5;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        // return PlaceholderFragment.newInstance(position + 1);
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new ZoomInFragment();
                break;
            case 1:
                fragment = new ZoomOutFragment();
                break;
            case 2:
                fragment = new SlideLRFragment();
                break;
            case 3:
                fragment = new SlideUDFragment();
                break;
            case 4:
                fragment = new TouchFragment();
        }
        return fragment;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getResources().getString(TAB_TITLES[position]);
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return numOfMode;
    }
}
