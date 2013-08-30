package fyp.hkust.doorposition;

import android.support.v4.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MapSectionFragment extends Fragment {

	public MapSectionFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.map,
				container, false);
		TextView titleTextView = (TextView) rootView
				.findViewById(R.id.view_map_title);
		//titleTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
		titleTextView.setText(R.string.title_map);
		titleTextView.setTextColor(getResources().getColor(R.color.light_grey));
		return rootView;
	}

}
