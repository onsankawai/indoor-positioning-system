package fyp.hkust.doorposition;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SettingsSectionFragment extends Fragment {
	public SettingsSectionFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.settings,
				container, false);
		TextView titleTextView = (TextView) rootView
				.findViewById(R.id.view_settings_title);
		//titleTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
		titleTextView.setText(R.string.title_settings);
		int i = 0;
		return rootView;
	}

	
}
