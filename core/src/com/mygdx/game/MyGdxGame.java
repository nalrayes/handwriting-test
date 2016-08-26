package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.awt.Image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MyGdxGame extends ApplicationAdapter {
	private SpriteBatch batch;
	private ShaderProgram shader;
	private Stage stage;
	private String vertexShader, fragmentShader;
	private SSIM ssim;
	private TakeScreenshot screenshot;

	private DrawablePixmap drawable;
	public MyGdxGame(SSIM type, TakeScreenshot takeScreenshot){
		ssim = type;
		screenshot = takeScreenshot;
	}


	class ScreenshotButtonActor extends Actor  {
		Texture manT = new Texture(Gdx.files.internal("man.9.png"));
		Sprite man = new Sprite (manT);

		ScreenshotButtonActor() {
			this.setWidth(man.getWidth());
			this.setHeight(man.getHeight());
			this.setBounds(this.getOriginX(), this.getOriginY(), getWidth(), getHeight());
			this.addListener(new InputListener() {
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttons) {
					File file = new File(Gdx.files.getExternalStoragePath(), "screenshot.png");

					System.out.println("FILE PATH: " + file.getAbsolutePath());
					System.out.println("FILE EXISTS:" + file.exists());

					FileHandle fh = Gdx.files.external("screenshot.png");

					saveScreenshotCropped(new FileHandle(file));

					System.out.println("FILE PATH: " + file.getAbsolutePath());
					System.out.println("FILE EXISTS:" + file.exists());

					return true;
				}
			});
		}

		public void draw(Batch batch, float alpha) {
			int scaleX = 250;
			int scaleY = 150;

			man.draw(batch);
			man.setPosition(Gdx.graphics.getWidth()/2 , Gdx.graphics.getHeight()/2);
		}

		public void takeScreenshot() {
			screenshot.takeScreenshot();
		}
		
		public void openScreenshot(File file) {
			screenshot.openScreenshot(file);
		}


	}

	public static void saveScreenshotCropped(FileHandle file) {
		Pixmap pixmap = getScreenshot(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		Pixmap toSave = new Pixmap(w, h/2, Format.RGBA8888);

		for (int x = w/2; x < w; x++) {
			for (int y = h/2; y < h; y++) {
				int colorInt = pixmap.getPixel(x, y);
				toSave.drawPixel(x - w/2, y - h/2, colorInt);
				// you could now draw that color at (x, y) of another pixmap of the size (regionWidth, regionHeight)
			}
		}
		pixmap.dispose();

		try {
			PixmapIO.writePNG(file, toSave);
			toSave.dispose();
		} catch (Exception e) {
			System.err.println("ERROR ERROR ERROR FUCK");
			e.printStackTrace();
			return;
		}

	}

	public static void saveScreenshot(FileHandle file) {
		Pixmap pixmap = getScreenshot(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		byte[] bytes;

		try {
			PixmapIO.writePNG(file, pixmap);
			pixmap.dispose();
		} catch (Exception e) {
			System.err.println("ERROR ERROR ERROR FUCK");
			e.printStackTrace();
			return;
		}
	}

	public static Pixmap getScreenshot(int x, int y, int w, int h, boolean flipY) {
		Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

		final Pixmap pixmap = new Pixmap(w, h, Format.RGBA8888);
		ByteBuffer pixels = pixmap.getPixels();
		Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);

		final int numBytes = w * h * 4;
		byte[] lines = new byte[numBytes];
		if (flipY) {
			final int numBytesPerLine = w * 4;
			for (int i = 0; i < h; i++) {
				pixels.position((h - i - 1) * numBytesPerLine);
				pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
			}
			pixels.clear();
			pixels.put(lines);
		} else {
			pixels.clear();
			pixels.get(lines);
		}

		return pixmap;
	}


	@Override
	public void create() {
		float ssimIndex = ssim.getSSIM("wrongsmallh.jpg", "rightsmallersmallh.jpg");
		System.out.println("SSIM: " + ssimIndex);
//		System.out.println("SSIM: " + ssimIndex);


		/* Some regular textures to draw on the scene. */
//		outline = new Texture("/home/nayef/Projects/handwriting-test/android/assets/smiley_outline.png");
//		color = new Texture("/home/nayef/Projects/handwriting-test/android/assets/smiley_color.png");

//		Pixmap backPix = new Pixmap(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()/2, Format.Alpha);
//		backPix.setColor(Color.BLACK);
//		backPix.fill();
//
//		back = new Texture(backPix);
//		backPix.dispose();


		/* I like to keep my shader programs as text files in the assets
		 * directory rather than dealing with horrid Java string format	ting. */
		vertexShader = "uniform mat4 u_projTrans;\n" +
				"\n" +
				"attribute vec4 a_position;\n" +
				"attribute vec4 a_color;\n" +
				"attribute vec2 a_texCoord0;\n" +
				"\n" +
				"varying vec4 v_color;\n" +
				"varying vec2 v_texCoord0;\n" +
				"\n" +
				"void main()\n" +
				"{\n" +
				"    v_color = a_color;\n" +
				"    v_texCoord0 = a_texCoord0;\n" +
				"    gl_Position = u_projTrans * a_position;\n" +
				"}\n";
		fragmentShader = "#ifdef GL_ES\n" +
				"    precision mediump float;\n" +
				"#endif\n" +
				"\n" +
				"uniform sampler2D u_texture;\n" +
				"uniform sampler2D u_mask;\n" +
				"\n" +
				"varying vec4 v_color;\n" +
				"varying vec2 v_texCoord0;\n" +
				"\n" +
				"void main()\n" +
				"{\n" +
				"    vec4 texColor = texture2D(u_texture, v_texCoord0);\n" +
				"    vec4 mask = texture2D(u_mask, v_texCoord0);\n" +
				"    texColor.a *= mask.a;\n" +
				"    gl_FragColor = v_color * texColor;\n" +
				"}";

		/* Bonus: you can set `pedantic = false` while tinkering with your
		 * shaders. This will stop it from crashing if you have unused variables
		 * and so on. */
		 ShaderProgram.pedantic = false;

		/* Construct our shader program. Spit out a log and quit if the shaders
		 * fail to compile. */
		shader = new ShaderProgram(vertexShader, fragmentShader);
		if (!shader.isCompiled()) {
			System.out.println("NOT COMPILING FUCK");
			Gdx.app.log("Shader", shader.getLog());
			Gdx.app.exit();
		}

		/* Construct a simple SpriteBatch using our shader program. */
		batch = new SpriteBatch();
//		batch.setShader(shader);
		Camera camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		stage = new Stage(new ScreenViewport(camera), batch);


		ScreenshotButtonActor button = new ScreenshotButtonActor();
		stage.addActor(button);
		button.setTouchable(Touchable.enabled);



		/* Tell our shader that u_texture will be in the TEXTURE0 spot and
		 * u_mask will be in the TEXTURE1 spot. We can set these now since
		 * they'll never change; we don't have to send them every render frame. */
		shader.begin();
		shader.setUniformi("u_texture", 0);
		shader.setUniformi("u_mask", 1);
		shader.end();

		/* Pixmap blending can result result in some funky looking lines when
		 * drawing. You may need to disable it. */
		Pixmap.setBlending(Blending.None);

		/* Construct our DrawablePixmap (custom class, defined below) with a
		 * Pixmap that is the dimensions of our screen. Alpha format is chosen
		 * because we are just using it as a mask and don't care about RGB color
		 * information. This will require less memory. */
		drawable = new DrawablePixmap(new Pixmap(Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight()/2, Format.Alpha), 1);

		class DrawableActor extends Actor {
			final DrawablePixmap drawablePixmap;
			DrawableActor(DrawablePixmap pix) {
				drawablePixmap  = pix;
			}

			@Override
			public void draw(Batch batch, float parentAlpha) {
				drawable.update();
				drawablePixmap.drawTexture(batch);
			}
		}

		DrawableActor drawableActor = new DrawableActor(drawable);
		stage.addActor(drawableActor);
		InputMultiplexer inputMultiplexer = new InputMultiplexer();

		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new DrawingInput());

		Gdx.input.setInputProcessor(inputMultiplexer);

	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		/* Update the mask texture (only if necessary). */
//		drawable.update();

		/* Color and outline are drawn separately here, but only to demonstrate
		 * this technique supports multiple images in a batch. */
//		batch.begin();
//		batch.draw(color, 0, 0);
//		batch.draw(outline, 0, 0);


		stage.act();

//		batch.draw(back, 0, 0);

//		stage.getBatch().begin();
//		stage.getActors().get(0).draw(batch, 0);
		stage.draw();


//		batch.end();


	}

	@Override
	public void dispose() {
		drawable.dispose();
//		outline.dispose();
//		color.dispose();
		batch.dispose();
	}

	/**
	 * Nested (static) class to provide a nice abstraction over Pixmap, exposing
	 * only the draw calls we want and handling some of the logic for smoothed
	 * (linear interpolated, aka 'lerped') drawing. This will become the 'owner'
	 * of the underlying pixmap, so it will need to be disposed.
	 */
	private class DrawablePixmap implements Disposable {

		private final int brushSize = 5;
		private final Color clearColor = new Color(0, 0, 0, 0);
		private final Color drawColor = new Color(1, 1, 1, 1);

		private Pixmap pixmap;
		private Texture texture;
		private boolean dirty;

		public DrawablePixmap(Pixmap pixmap, int textureBinding) {
			this.pixmap = pixmap;
			pixmap.setColor(drawColor);

			/* Create a texture which we'll update from the pixmap. */
			this.texture = new Texture(pixmap);
			this.dirty = false;

			/* Bind the mask texture to TEXTURE<N> (TEXTURE1 for our purposes),
			 * which also sets the currently active texture unit. */
			this.texture.bind(textureBinding);

			/* However SpriteBatch will auto-bind to the current active texture,
			 * so we must now reset it to TEXTURE0 or else our mask will be
			 * overwritten. */
			Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		}

		/** Write the pixmap onto the texture if the pixmap has changed. */
		public void update() {
			if (dirty) {
				texture.draw(pixmap, 0, 0);
//				batch.begin();
//				batch.draw(texture, 0, 0);
//				batch.end();
				dirty = false;
			}
//			FileHandle fh  = Gdx.files.internal("screenshot.png");
//
//			saveScreenshot(fh);
		}

		public void clear() {
			pixmap.setColor(clearColor);
			pixmap.fill();
			pixmap.setColor(drawColor);
			dirty = true;
		}

		private void drawDot(Vector2 spot) {
			pixmap.fillCircle((int) spot.x, (int) spot.y - Gdx.graphics.getHeight()/2, brushSize);
//			System.out.println("spot.x: " + spot.x);
//			System.out.println("spot.y: " + spot.y);
		}

		public void draw(Vector2 spot) {
			drawDot(spot);
			dirty = true;
		}

		public void drawLerped(Vector2 from, Vector2 to) {
			float dist = to.dst(from);
			/* Calc an alpha step to put one dot roughly every 1/8 of the brush
			 * radius. 1/8 is arbitrary, but the results are fairly nice. */
			float alphaStep = brushSize / (8f * dist);

			for (float a = 0; a < 1f; a += alphaStep) {
				Vector2 lerped = from.lerp(to, a);
				drawDot(lerped);
			}

			drawDot(to);
			dirty = true;
		}

		@Override
		public void dispose() {
			texture.dispose();
			pixmap.dispose();
		}

		public void drawTexture(Batch batch) {
			batch.draw(texture, 0, 0);
		}
	}

	/**
	 * Inner (non-static) class to handle mouse and keyboard events. Mostly we
	 * want to pass on appropriate draw calls to our DrawablePixmap and this
	 * means keeping track of some state (last coordinates drawn and whether or
	 * not the left mouse button is pressed) to handle smooth drawing while
	 * dragging the mouse.
	 */
	private class DrawingInput extends InputAdapter {

		private Vector2 last = null;
		private boolean leftDown = false;

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer,
								 int button) {
			if (button == Input.Buttons.LEFT) {
				Vector2 curr = new Vector2(screenX, screenY);
				drawable.draw(curr);
				last = curr;
				leftDown = true;
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			if (leftDown) {
				Vector2 curr = new Vector2(screenX, screenY);
				drawable.drawLerped(last, curr);
				last = curr;
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			if (button == Input.Buttons.LEFT) {
				drawable.draw(new Vector2(screenX, screenY));
				last = null;
				leftDown = false;
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean keyDown(int keycode) {
			switch (keycode) {
				case Input.Keys.ESCAPE:
				case Input.Keys.F5:
					return true;
				default:
					return false;
			}
		}

		@Override
		public boolean keyUp(int keycode) {
			switch (keycode) {
				case Input.Keys.ESCAPE:
					Gdx.app.exit();
					return true;
				case Input.Keys.F5:
					drawable.clear();
					return true;
				default:
					return false;
			}
		}
	}
}
