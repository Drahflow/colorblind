package name.drahflow.colorblind;

import android.app.*;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.ImageFormat;
import android.graphics.RectF;
import android.widget.*;
import android.view.*;
import android.util.*;
import android.content.*;
import android.hardware.*;
import java.util.*;
import java.io.IOException;

public class MainSurface extends View {
	private Colorblind ctx;
	private Camera camera;
	public SurfaceView preview;
	private SurfaceHolder previewHolder;

	private int width;
	private int height;
	private int currentColor;

	public MainSurface(Colorblind context) {
		super(context);
		this.ctx = context;
		this.preview = new SurfaceView(ctx);
	}

	public void startActive() {
		if(previewHolder == null) {
			previewHolder = preview.getHolder();
			previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			previewHolder.addCallback(new SurfaceHolder.Callback() {
				public void surfaceCreated(SurfaceHolder holder) {
					// no-op -- wait until surfaceChanged()
				}
				
				public void surfaceChanged(SurfaceHolder holder,
																	 int format, int width,
																	 int height) {
					Log.i("Colorblind", "here2");
					if(camera == null) {
						camera = Camera.open();

						try {
							camera.setPreviewDisplay(previewHolder);
						} catch(IOException ioe) {
							throw new Error("Camera init failed", ioe);
						}
						camera.setPreviewCallback(previewCallback);

						Camera.Parameters params = camera.getParameters();
						params.setPreviewFormat(ImageFormat.NV21);
						camera.setParameters(params);

						camera.startPreview();
					}
				}
				
				public void surfaceDestroyed(SurfaceHolder holder) {
					// no-op
				}
			});

			Log.i("Colorblind", "here");
		}
	}

	public void stopActive() {
		if(camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

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

	private static RectF colorWheel = new RectF(20, 10, 120, 110);

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

		final int yOffset = 15;
		final float chromaAmp = 1.5f;

		final int cyMin = (height / 2 - 10) * canvasHeight / height;
		final int cyMax = (height / 2 + 10) * canvasHeight / height;
		final int cxMin = (width / 2 - 10) * canvasWidth / width;
		final int cxMax = (width / 2 + 10) * canvasWidth / width;
		for(int cy = cyMin; cy < cyMax; ++cy) {
			for(int cx = cxMin; cx < cxMax; ++cx) {
				final int x = cx * width / canvasWidth;
				final int y = cy * height / canvasHeight + yOffset;

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

				if(y > height / 2 - 2 + yOffset && y < height / 2 + 2 + yOffset && x > width / 2 - 5 && x < width / 2 + 5) {
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

		paint.setColor(0xff000000);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(0, 0, 150, canvasHeight, paint);

		paint.setColor(currentColor);
		paint.setStrokeWidth(30);
		canvas.drawPoint(10, 10, paint);

		paint.setColor(0xffffffff);
		paint.setStrokeWidth(2);
		paint.setStyle(Paint.Style.STROKE);

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

		float hueAngle = (float)(hue * Math.PI / 3);

		canvas.drawOval(colorWheel, paint);
		canvas.drawLine(
				colorWheel.centerX(),
				colorWheel.centerY(),
				colorWheel.centerX() + colorWheel.width() * (float)Math.cos(hueAngle) / 2,
				colorWheel.centerY() - colorWheel.height() * (float)Math.sin(hueAngle) / 2,
				paint
			);
		canvas.drawLine(
				colorWheel.centerX() + chromaAmp * chroma * colorWheel.width() * (float)Math.cos(hueAngle - 0.2) / 2 / 256,
				colorWheel.centerY() - chromaAmp * chroma * colorWheel.height() * (float)Math.sin(hueAngle - 0.2) / 2 / 256,
				colorWheel.centerX() + chromaAmp * chroma * colorWheel.width() * (float)Math.cos(hueAngle + 0.2) / 2 / 256,
				colorWheel.centerY() - chromaAmp * chroma * colorWheel.height() * (float)Math.sin(hueAngle + 0.2) / 2 / 256,
				paint
			);

		canvas.drawText("R", colorWheel.right + 10, colorWheel.centerY(), paint);
		canvas.drawText("G", colorWheel.left, colorWheel.top, paint);
		canvas.drawText("B", colorWheel.left, colorWheel.bottom, paint);
	}
}
