package physica.library.util;

import physica.api.core.abstraction.Face;

public class RotationUtility {

	public static final int[][] relativeMatrix = new int[][] { new int[] { 3, 2, 1, 0, 5, 4 }, new int[] { 4, 5, 0, 1, 2, 3 }, new int[] { 0, 1, 3, 2, 4, 5 }, new int[] { 0, 1, 2, 3, 5, 4 }, new int[] { 0, 1, 5, 4, 3, 2 },
			new int[] { 0, 1, 4, 5, 2, 3 } };

	public static Face getRelativeSide(Face relativeDirection, Face currentDirection)
	{
		if (relativeDirection == Face.UNKNOWN || currentDirection == Face.UNKNOWN)
		{
			return Face.UNKNOWN;
		}
		return Face.getOrientation(relativeMatrix[currentDirection.ordinal()][relativeDirection.ordinal()]);
	}
}