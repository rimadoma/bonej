package org.doube.geometry;

import org.doube.jama.EigenvalueDecomposition;
import org.doube.jama.Matrix;

/**
 * Ellipse-fitting methods.
 * 
 * @author Michael Doube
 * 
 */
public class FitEllipse {

	/**
	 * Java port of Chernov's MATLAB implementation of the direct ellipse fit
	 * 
	 * @param points
	 *            n * 2 array of 2D coordinates.
	 * @return <p>
	 *         6-element array, {a b c d e f}, which are the algebraic
	 *         parameters of the fitting ellipse: <i>ax</i><sup>2</sup> +
	 *         <i>bxy</i> + <i>cy</i><sup>2</sup> +<i>dx</i> + <i>ey</i> +
	 *         <i>f</i> = 0. The vector <b>A</b> represented in the array is
	 *         normed, so that ||<b>A</b>||=1.
	 *         </p>
	 * 
	 * @see <p>
	 *      <a href="http://www.mathworks.co.uk/matlabcentral/fileexchange/22684-ellipse-fit-direct-method"
	 *      >MATLAB script</a>
	 *      </p>
	 */
	public static double[] direct(double[][] points) {
		final int nPoints = points.length;
		double[] centroid = Centroid.getCentroid(points);
		final double xC = centroid[0];
		final double yC = centroid[1];
		double[][] d1 = new double[nPoints][3];
		for (int i = 0; i < nPoints; i++) {
			final double xixC = points[i][0] - xC;
			final double yiyC = points[i][1] - yC;
			d1[i][0] = xixC * xixC;
			d1[i][1] = xixC * yiyC;
			d1[i][2] = yiyC * yiyC;
		}
		Matrix D1 = new Matrix(d1);
		double[][] d2 = new double[nPoints][3];
		for (int i = 0; i < nPoints; i++) {
			d2[i][0] = points[i][0] - xC;
			d2[i][1] = points[i][1] - yC;
			d2[i][2] = 1;
		}
		Matrix D2 = new Matrix(d2);

		Matrix S1 = D1.transpose().times(D1);

		Matrix S2 = D1.transpose().times(D2);

		Matrix S3 = D2.transpose().times(D2);

		Matrix T = (S3.inverse().times(-1)).times(S2.transpose());

		Matrix M = S1.plus(S2.times(T));

		double[][] m = M.getArray();
		double[][] n = { { m[2][0] / 2, m[2][1] / 2, m[2][2] / 2 },
				{ -m[1][0], -m[1][1], -m[1][2] },
				{ m[0][0] / 2, m[0][1] / 2, m[0][2] / 2 } };

		Matrix N = new Matrix(n);

		EigenvalueDecomposition E = N.eig();
		Matrix eVec = E.getV();

		Matrix R1 = eVec.getMatrix(0, 0, 0, 2);
		Matrix R2 = eVec.getMatrix(1, 1, 0, 2);
		Matrix R3 = eVec.getMatrix(2, 2, 0, 2);

		Matrix cond = (R1.times(4)).arrayTimes(R3).minus(R2.arrayTimes(R2));

		int f = 0;
		for (int i = 0; i < 3; i++) {
			if (cond.get(0, i) > 0) {
				f = i;
				break;
			}
		}
		Matrix A1 = eVec.getMatrix(0, 2, f, f);

		Matrix A = new Matrix(6, 1);
		A.setMatrix(0, 2, 0, 0, A1);
		A.setMatrix(3, 5, 0, 0, T.times(A1));

		double[] a = A.getColumnPackedCopy();
		double a4 = a[3] - 2 * a[0] * xC - a[1] * yC;
		double a5 = a[4] - 2 * a[2] * yC - a[1] * xC;
		double a6 = a[5] + a[0] * xC * xC + a[2] * yC * yC + a[1] * xC * yC
				- a[3] * xC - a[4] * yC;
		A.set(3, 0, a4);
		A.set(4, 0, a5);
		A.set(5, 0, a6);
		A = A.times(1 / A.normF());
		return A.getColumnPackedCopy();
	}

	/**
	 * Ellipse fit by Taubin's Method published in G. Taubin, "Estimation Of
	 * Planar Curves, Surfaces And Nonplanar Space Curves Defined By Implicit
	 * Equations, With Applications To Edge And Range Image Segmentation", IEEE
	 * Trans. PAMI, Vol. 13, pages 1115-1138, (1991)
	 * 
	 * CURRENTLY NOT WORKING - USE FitEllipse.direct(double[]) INSTEAD
	 * 
	 * Ported from Chernov's Matlab script.
	 * 
	 * Input: points[n][2] is the array of coordinates of n points
	 * x(i)=points[i][0], y(i)=points[i][1]
	 * 
	 * Output: A = [a b c d e f]' is the vector of algebraic parameters of the
	 * fitting ellipse: ax^2 + bxy + cy^2 +dx + ey + f = 0 the vector A is
	 * normed, so that ||A||=1
	 * 
	 * Among fast non-iterative ellipse fitting methods, this is perhaps the
	 * most accurate and robust
	 * 
	 * Note: this method fits a quadratic curve (conic) to a set of points; if
	 * points are better approximated by a hyperbola, this fit will return a
	 * hyperbola. To fit ellipses only, use "Direct Ellipse Fit".
	 * 
	 * @author Michael Doube
	 * @param points
	 * @return
	 * @see <p>
	 *      <a href="http://www.mathworks.co.uk/matlabcentral/fileexchange/22683-ellipse-fit-taubin-method"
	 *      >MATLAB script</a>
	 *      </p>
	 */
	public static double[] taubin(double[][] points) {

		final int nPoints = points.length;

		// centroid = mean(XY); % the centroid of the data set
		double[] centroid = Centroid.getCentroid(points);
		final double xC = centroid[0];
		final double yC = centroid[1];

		// Z = [(XY(:,1)-centroid(1)).^2,
		// (XY(:,1)-centroid(1)).*(XY(:,2)-centroid(2)),
		// (XY(:,2)-centroid(2)).^2,
		// XY(:,1)-centroid(1),
		// XY(:,2)-centroid(2),
		// ones(size(XY,1),1)];

		double[][] z = new double[nPoints][6];
		for (int i = 0; i < nPoints; i++) {
			final double xixC = points[i][0] - xC;
			final double yiyC = points[i][1] - yC;
			z[i][0] = xixC * xixC;
			z[i][1] = xixC * yiyC;
			z[i][2] = yiyC * yiyC;
			z[i][3] = xixC;
			z[i][4] = yiyC;
			z[i][5] = 1;
		}
		Matrix Z = new Matrix(z);

		// M = Z'*Z/size(XY,1);
		Matrix M = Z.transpose().times(Z.times(1 / (double) nPoints));

		double[][] m = M.getArray();

		//
		// P = [M(1,1)-M(1,6)^2, M(1,2)-M(1,6)*M(2,6), M(1,3)-M(1,6)*M(3,6),
		// M(1,4), M(1,5);

		// M(1,2)-M(1,6)*M(2,6), M(2,2)-M(2,6)^2, M(2,3)-M(2,6)*M(3,6), M(2,4),
		// M(2,5);

		// M(1,3)-M(1,6)*M(3,6), M(2,3)-M(2,6)*M(3,6), M(3,3)-M(3,6)^2, M(3,4),
		// M(3,5);

		// M(1,4), M(2,4), M(3,4), M(4,4), M(4,5);

		// M(1,5), M(2,5), M(3,5), M(4,5), M(5,5)];
		double[][] p = new double[5][5];
		p[0][0] = m[0][0] - m[0][5] * m[0][5];
		p[0][1] = m[0][1] - m[0][5] * m[1][5];
		p[0][2] = m[0][2] - m[0][5] * m[2][5];
		p[0][3] = m[0][3];
		p[0][4] = m[0][4];

		p[1][0] = m[0][1] - m[0][5] * m[1][5];
		p[1][1] = m[1][1] - m[1][5] * m[1][5];
		p[1][2] = m[1][2] - m[1][5] * m[2][5];
		p[1][3] = m[1][3];
		p[1][4] = m[1][4];

		p[2][0] = m[0][2] - m[0][5] * m[2][5];
		p[2][1] = m[1][2] - m[1][5] * m[2][5];
		p[2][2] = m[2][2] - m[2][5] * m[2][5];
		p[2][3] = m[2][3];
		p[2][4] = m[2][4];

		p[3][0] = m[0][3];
		p[3][1] = m[1][3];
		p[3][2] = m[2][3];
		p[3][3] = m[3][3];
		p[3][4] = m[3][4];

		p[4][0] = m[0][4];
		p[4][1] = m[1][4];
		p[4][2] = m[2][4];
		p[4][3] = m[3][4];
		p[4][4] = m[4][4];

		Matrix P = new Matrix(p);

		// Q = [4*M(1,6), 2*M(2,6), 0, 0, 0;
		// 2*M(2,6), M(1,6)+M(3,6), 2*M(2,6), 0, 0;
		// 0, 2*M(2,6), 4*M(3,6), 0, 0;
		// 0, 0, 0, 1, 0;
		// 0, 0, 0, 0, 1];
		double[][] q = { { 4 * m[0][5], 2 * m[2][5], 0, 0, 0 },
				{ 2 * m[1][5], m[0][5] + m[2][5], 2 * m[1][5], 0, 0 },
				{ 0, 2 * m[1][5], 4 * m[2][5], 0, 0 }, { 0, 0, 0, 1, 0 },
				{ 0, 0, 0, 0, 1 } };

		Matrix Q = new Matrix(q);

		// [V,D] = eig(P,Q);
		EigenvalueDecomposition E = new EigenvalueDecomposition(P.times(Q
				.inverse()));
		Matrix V = E.getV();
		Matrix D = E.getD();

		// [Dsort,ID] = sort(diag(D));
		double[] ds = D.diag().getColumnPackedCopy();
		int j = 0;
		for (int i = 0; i < ds.length; i++) {
			if (ds[i] < ds[j]) {
				j = i;
			}
		}

		// A = V(:,ID(1));
		Matrix A = V.getMatrix(0, 4, j, j);

		// A = [A; -A(1:3)'*M(1:3,6)];
		Matrix A13 = A.getMatrix(0, 2, 0, 0).times(-1);
		Matrix M136 = M.getMatrix(0, 2, 5, 5);
		Matrix AM = A13.inverse().times(M136);
		Matrix AA = new Matrix(6, 1);
		AA.setMatrix(0, 4, 0, 0, A);
		AA.setMatrix(5, 5, 0, 0, AM);

		// A4 = A(4)-2*A(1)*centroid(1)-A(2)*centroid(2);
		double[] a = AA.getColumnPackedCopy();
		double a4 = a[3] - 2 * a[0] * xC - a[1] * yC;

		// A5 = A(5)-2*A(3)*centroid(2)-A(2)*centroid(1);
		double a5 = a[4] - 2 * a[2] * yC - a[1] * xC;

		// A6 = A(6)+A(1)*centroid(1)^2+A(3)*centroid(2)^2+...
		// A(2)*centroid(1)*centroid(2)-A(4)*centroid(1)-A(5)*centroid(2);
		double a6 = a[5] + a[0] * xC * xC + a[2] * yC * yC + a[1] * xC * yC
				- a[3] * xC - a[4] * yC;

		// A(4) = A4; A(5) = A5; A(6) = A6;
		AA.set(3, 0, a4);
		AA.set(4, 0, a5);
		AA.set(5, 0, a6);

		// A = A/norm(A);
		AA = AA.times(1 / AA.normF());

		// end % Taubin
		return AA.getColumnPackedCopy();
	}

	/**
	 * Create an array of (x,y) coordinates on an ellipse of radii (a,b) and
	 * rotated r radians. Random noise is added if noise > 0.
	 * 
	 * @param a
	 *            One radius
	 * @param b
	 *            The other radius
	 * @param r
	 *            Angle of rotation
	 * @param c
	 *            centroid x
	 * @param d
	 *            centroid y
	 * @param noise
	 *            intensity of random noise
	 * @param n
	 *            Number of points
	 * @return
	 */
	public static double[][] testEllipse(double a, double b, double r,
			double c, double d, double noise, int n) {
		double[][] points = new double[n][2];
		// alpha=linspace(0,2*pi,200);
		double increment = 2 * Math.PI / (n + 1);
		double alpha = 0;
		for (int i = 0; i < n; i++) {
			// x=5*cos(alpha);
			// y=2*sin(alpha);
			points[i][0] = a * Math.cos(alpha) + Math.random() * noise;
			points[i][1] = b * Math.sin(alpha) + Math.random() * noise;
			alpha += increment;
		}

		// xyr=[cos(phi) -sin(phi);sin(phi) cos(phi)]*[x(:) y(:)];
		final double sinR = Math.sin(r);
		final double cosR = Math.cos(r);
		for (int i = 0; i < n; i++) {
			final double x = points[i][0];
			final double y = points[i][1];
			points[i][0] = x * cosR - y * sinR + c;
			points[i][1] = x * sinR + y * cosR + d;
		}
		return points;
	}

	/**
	 * <p>
	 * Convert variables a, b, c, d, f, g from the general ellipse equation ax^2
	 * + bxy + cy^2 +dx + fy + g = 0 into useful geometric parameters semi-axis
	 * lengths, centre and angle of rotation.
	 * </p>
	 * 
	 * @see <p>
	 *      Eq. 19-23 at <a
	 *      href="http://mathworld.wolfram.com/Ellipse.html">Wolfram Mathworld
	 *      Ellipse</a>.
	 *      </p>
	 * 
	 * @param ellipse
	 *            <p>
	 *            array containing a, b, c, d, f, g of the ellipse equation.
	 *            </p>
	 * @return <p>
	 *         array containing centroid coordinates, axis lengths and angle of
	 *         rotation of the ellipse specified by the input variables.
	 *         </p>
	 */
	public static double[] varToDimensions(double[] ellipse) {
		final double a = ellipse[0];
		final double b = ellipse[1] / 2;
		final double c = ellipse[2];
		final double d = ellipse[3] / 2;
		final double f = ellipse[4];
		final double g = ellipse[5];

		// centre
		final double cX = (c * d - b * f) / (b * b - a * c);
		final double cY = (a * f - b * d) / (b * b - a * c);

		// semiaxis length
		final double af = 2 * (a * f * f + c * d * d + g * b * b - 2 * b * d
				* f - a * c * g);

		final double aL = Math.sqrt((af)
				/ ((b * b - a * c) * (Math.sqrt((a - c) * (a - c) + 4 * b * b)
						- a - c)));

		final double bL = Math.sqrt((af)
				/ ((b * b - a * c) * (-Math.sqrt((a - c) * (a - c) + 4 * b * b)
						- a - c)));
		double phi = 0;
		if (b == 0) {
			if (a <= c)
				phi = 0;
			else if (a > c)
				phi = Math.PI / 2;
		} else {
			if (a < c)
				phi = Math.atan(2 * b / (a - c)) / 2;
			else if (a > c)
				phi = Math.atan(2 * b / (a - c)) / 2 + Math.PI / 2;
		}
		double[] dimensions = { cX, cY, aL, bL, phi };
		return dimensions;
	}
}
