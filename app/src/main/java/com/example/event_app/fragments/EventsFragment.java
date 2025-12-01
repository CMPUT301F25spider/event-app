package com.example.event_app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.event_app.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * EventsFragment - Hosts the two main event tabs inside the app:
 * <ul>
 *     <li><b>Browse Events</b> – Users can explore all active events</li>
 *     <li><b>My Events</b> – Shows events organized by the logged-in user</li>
 * </ul>
 *
 * The fragment uses a ViewPager2 + TabLayout setup to allow smooth
 * swiping between tabs.
 */
public class EventsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    /**
     * Inflates the layout containing the TabLayout and ViewPager2.
     *
     * @param inflater LayoutInflater used to inflate the fragment UI
     * @param container optional parent container
     * @param savedInstanceState saved view state bundle
     * @return the inflated root view for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    /**
     * Called after the fragment view is created.
     * Initializes the TabLayout, ViewPager2, sets up the pager adapter,
     * and attaches the tabs using a TabLayoutMediator.
     *
     * @param view the root view of the fragment
     * @param savedInstanceState previously saved instance state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        // Setup ViewPager2 with adapter
        EventsPagerAdapter adapter = new EventsPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Browse Events");
                    break;
                case 1:
                    tab.setText("My Events");
                    break;
            }
        }).attach();
    }

    /**
     * Adapter for ViewPager2 inside EventsFragment.
     * Creates the correct fragment for each tab position:
     * <ul>
     *     <li>0 → BrowseEventsTabFragment</li>
     *     <li>1 → MyOrganizedEventsTabFragment</li>
     * </ul>
     */
    private static class EventsPagerAdapter extends FragmentStateAdapter {

        /**
         * Creates a new pager adapter that will manage tab fragments.
         *
         * @param fragment the parent fragment hosting this ViewPager2
         */
        public EventsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        /**
         * Returns the fragment to display for a given tab position.
         *
         * @param position the index of the selected tab
         * @return a fragment instance matching the tab:
         *         <br>0 → BrowseEventsTabFragment
         *         <br>1 → MyOrganizedEventsTabFragment
         */
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new BrowseEventsTabFragment();
                case 1:
                    return new MyOrganizedEventsTabFragment();
                default:
                    return new BrowseEventsTabFragment();
            }
        }

        /**
         * @return the total number of tabs in the ViewPager2 (2 tabs)
         */
        @Override
        public int getItemCount() {
            return 2; // Two tabs
        }
    }
}