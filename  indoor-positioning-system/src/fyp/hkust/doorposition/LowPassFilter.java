package fyp.hkust.doorposition;

import java.util.List;

public class LowPassFilter {
	private LowPassFilter(){};
	private static float SMOOTHING_CONSTANT = 148.0f; // 36, 148
	
	public static void smoothValues(List<float[]> values, int start) {
		float[] value = (float[]) values.get(start);
		for (int i = start + 1; i < start + values.size(); i++) {
			float[] currentValue = (float[]) values.get(i%values.size());
			value[0] += (currentValue[0] - value[0]) / SMOOTHING_CONSTANT;
			values.set(i%values.size(), value);
		}
		
	}
}
