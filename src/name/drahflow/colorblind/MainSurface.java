package name.drahflow.colorblind;

import android.app.*;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.ImageFormat;
import android.widget.*;
import android.view.*;
import android.content.*;
import android.hardware.*;
import java.util.*;

public class MainSurface extends View {
	private Colorblind ctx;
	private Camera camera;
	private SurfaceHolder previewHolder;

	private int width;
	private int height;
	private int currentColor;

	public MainSurface(Colorblind context) {
		super(context);
		this.ctx = context;

		camera = Camera.open();
		camera.setPreviewCallback(previewCallback);

		Camera.Parameters params = camera.getParameters();
		params.setPreviewFormat(ImageFormat.NV21);
		params.setPreviewSize(width, height);
		camera.setParameters(params);
		camera.startPreview();
	}

			//camera.stopPreview();
			//camera.release();

	private List<Integer> reds = new ArrayList<Integer>();
	private List<Integer> greens = new ArrayList<Integer>();
	private List<Integer> blues = new ArrayList<Integer>();

	private byte[] data;

	Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera camera) {
			MainSurface.this.data = new byte[data.length];
			System.arraycopy(data, 0, MainSurface.this.data, 0, data.length);

			Camera.Size previewSize = camera.getParameters().getPreviewSize();
			MainSurface.this.width = previewSize.width;
			MainSurface.this.height = previewSize.height;

			invalidate();
		}
	};

	private Paint paint = new Paint();

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if(data == null) return;

		final int width = this.width;
		final int height = this.height;

		final int canvasWidth = canvas.getWidth();
		final int canvasHeight = canvas.getHeight();

		reds.clear();
		greens.clear();
		blues.clear();

		paint.setStrokeWidth(1);

		for(int cy = 0; cy < canvasHeight; ++cy) {
			for(int cx = 0; cx < canvasWidth; ++cx) {
				final int x = cx * width / canvasWidth;
				final int y = cy * width / canvasWidth;

				final int Yi = y * width + x;
				final int Vi = width * height + (y / 2) * width + (x / 2) * 2;
				final int Ui = width * height + (y / 2) * width + (x / 2) * 2 + 1;

				if(Yi >= data.length - 1) break;
				if(Ui >= data.length - 1) break;
				if(Vi >= data.length - 1) break;

				float Y = data[Yi];
				float U = data[Ui];
				float V = data[Vi];
				if(Y < 0) Y += 256;
				if(U < 0) U += 256;
				if(V < 0) V += 256;

				int b = (int)(1.164*(Y - 16)                   + 2.018*(U - 128));
				int g = (int)(1.164*(Y - 16) - 0.813*(V - 128) - 0.391*(U - 128));
				int r = (int)(1.164*(Y - 16) + 1.596*(V - 128));

				if(r < 0) r = 0;
				if(r > 255) r = 255;
				if(g < 0) g = 0;
				if(g > 255) g = 255;
				if(b < 0) b = 0;
				if(b > 255) b = 255;

				currentColor = 0xff000000 |
					0x010000 * r |
					0x000100 * g |
					0x000001 * b;

				if(y > height / 2 - 2 && y < height / 2 + 2 && x > width / 2 - 5 && x < width / 2 + 5) {
					currentColor = 0xffffffff;

					reds.add(r);
					greens.add(g);
					blues.add(b);
				}

				paint.setColor(currentColor);
				canvas.drawPoint(cx, cy, paint);
			}
		}

		Collections.sort(reds);
		Collections.sort(greens);
		Collections.sort(blues);

		int red = reds.get(reds.size() / 2);
		int green = greens.get(greens.size() / 2);
		int blue = blues.get(blues.size() / 2);

		currentColor = 0xff000000 |
			red * 0x010000 |
			green * 0x000100 |
			blue * 0x000001;

		paint.setColor(currentColor);
		paint.setStrokeWidth(30);
		canvas.drawPoint(10, 10, paint);

		paint.setColor(0xffffffff);
		paint.setStrokeWidth(1);

		canvas.drawText("R", 10, canvasHeight - 60, paint);
		canvas.drawText("G", 20, canvasHeight - 60, paint);
		canvas.drawText("B", 30, canvasHeight - 60, paint);

		canvas.drawLine(10, canvasHeight - 70, 10, canvasHeight - 70 - red * (canvasHeight - 90) / 256, paint);
		canvas.drawLine(20, canvasHeight - 70, 20, canvasHeight - 70 - green * (canvasHeight - 90) / 256, paint);
		canvas.drawLine(30, canvasHeight - 70, 30, canvasHeight - 70 - blue * (canvasHeight - 90) / 256, paint);

		int max = red;
		int min = red;
		if(green > max) max = green;
		if(green < min) min = green;
		if(blue > max) max = blue;
		if(blue < min) min = blue;

		int chroma = max - min;
		float hue = 0;

		if(chroma != 0) {
			if(max == red) {
				hue = ((float)(green - blue) / chroma);
				if(hue < 0) hue += 6;
			} else if(max == green) {
				hue = ((float)(blue - red) / chroma) + 2;
			} else {
				hue = ((float)(red - green) / chroma) + 4;
			}
		}

		String color = "???";

		if(hue > 5.5 || hue < 0.5) {
			color = "red";
		} else if(hue < 1.5) {
			color = "yellow";
		} else if(hue < 2.5) {
			color = "green";
		} else if(hue < 3.5) {
			color = "cyan";
		} else if(hue < 4.5) {
			color = "blue";
		} else if(hue < 5.5) {
			color = "blue";
		}

		canvas.drawText(color, 30, 10, paint);
	}
}
