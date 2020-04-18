package com.example.memorycanvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class Card {
    Bitmap picture, backColor;
    int path2pic;
    boolean isOpen = false; // цвет карты
    float x, y, width, height;

    public Card(float x, float y, float width, float height, Bitmap picture, int path, Bitmap backColor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.picture = picture;
        this.path2pic = path;
        this.backColor = backColor;
    }

    public void draw(Canvas c) {
        if (isOpen) {
            c.drawBitmap(picture, x, y, null);
        } else c.drawBitmap(backColor, x, y, null);
    }

    public boolean flip(float touch_x, float touch_y) {
        if (touch_x >= x && touch_x <= x + width && touch_y >= y && touch_y <= y + height) {
            isOpen = !isOpen;
            return true;
        } else return false;
    }
}


public class TilesView extends View {
    // пауза для запоминания карт
    final int PAUSE_LENGTH = 1; // в секундах
    boolean isOnPauseNow = false;

    // число открытых карт
    int openedCard = 0;
    ArrayList<Card> cards = new ArrayList<>();
    int width, height; // ширина и высота канвы

    boolean init_field = false;
    int k1 = 4, k2 = 3; // 4x3
    int[][] tiles;  // массив 2*n адресов картинок
    int firstCard, secondCard;
    boolean stop_draw = false;

    public TilesView(Context context) {
        super(context);
    }

    public TilesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // сгенерировать поле 2*n карт, при этом
        // должно быть ровно n пар карт разных картинок

        int[] rgb = new int[]{R.drawable.card1, R.drawable.card2, R.drawable.card3,
                R.drawable.card4, R.drawable.card5, R.drawable.card6};
        int[] pair = new int[rgb.length];
        Arrays.fill(pair, 2);

        int k;
        tiles = new int[k1][k2];
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[i].length; j++) {
                while (tiles[i][j] == 0) {
                    Random r = new Random();
                    k = r.nextInt(rgb.length);
                    if (pair[k] > 0) {
                        tiles[i][j] = rgb[k];
                        pair[k]--;
                    }
                }
            }
        }
    }

    public void init(int w, int h) {
        int a = (int) (Math.min(w, h) * 0.8 / 3); //length
        int b = (int) (Math.max(w, h) * 0.8 / 4); //height
        int offset = (int) (Math.min(w, h) * 0.1 / 4); //offset_between
        int f1 = (int) ((Math.min(w, h) - a * k2 - offset * (k2 - 1)) / 2); //offset_for_center
        int f2 = (int) ((Math.max(w, h) - b * k1 - offset * (k1 - 1)) / 2); //offset_for_center

        Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.card_back);
        Bitmap back = Bitmap.createScaledBitmap(img,a,b,false);
        for (int i = 0; i < k1; i++) {
            for (int j = 0; j < k2; j++) {
                Bitmap image = BitmapFactory.decodeResource(getResources(), tiles[i][j]);
                Bitmap pic = Bitmap.createScaledBitmap(image, a, b, false);
                cards.add(new Card((a + offset) * j + f1, (b + offset) * i + f2, a, b, pic, tiles[i][j], back));
            }
        }
    }

    public void checkOpenCardsEqual(int p1, int p2) {
        Card c1 = null, c2 = null;
        if (p1 == p2) {
            int k = 2;
            for (Card c : cards) {
                if (c.path2pic == p1 && k == 2) {
                    c1 = c;
                    k--;
                }else if (c.path2pic == p1 && k == 1) {
                    c2 = c;
                    k--;
                    break;
                }
            }
            cards.remove(c1);
            cards.remove(c2);
        }
    }

    public void setWinBackground() {
        setBackground(getResources().getDrawable(R.drawable.bg_congratulation));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        width = canvas.getWidth();
        height = canvas.getHeight();

        // отрисовка плиток
        if (!init_field) {
            init(width, height);
            init_field = true;
        }

        // проверить, остались ли ещё карты
        // иначе сообщить об окончании игры
        if (cards.size() > 0) {
            for (Card c : cards) {
                c.draw(canvas);
            }
        } else {
            if (!stop_draw) {
                stop_draw = true;
                setWinBackground();
                Toast.makeText(getContext(), "Поздравляем!!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // координаты касания
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN && !isOnPauseNow) {
            // палец коснулся экрана
            for (Card c : cards) {
                if (openedCard == 0) {
                    if (c.flip(x, y)) {
                        Log.d("mytag", "card flipped: " + openedCard);
                        openedCard++;
                        invalidate();
                        firstCard = c.path2pic;
                        return true;
                    }
                }

                if (openedCard == 1) {
                    // перевернуть карту с задержкой
                    if (c.flip(x, y)) {
                        openedCard++;
                        secondCard = c.path2pic;
                        invalidate();
                        PauseTask task = new PauseTask();
                        task.execute(PAUSE_LENGTH);
                        isOnPauseNow = true;
                        return true;
                    }
                }
            }
        }
        return true;
    }

    public void newGame() {
        // запуск новой игры
        TilesView tl = new TilesView(getContext(), null);
        init_field = false;
        this.tiles = tl.tiles;
        this.cards = tl.cards;
        setBackgroundColor(Color.rgb(224, 255, 255));
        stop_draw = false;
        invalidate();
    }

    class PauseTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... integers) {
            Log.d("mytag", "Pause started");
            try {
                Thread.sleep(integers[0] * 1000); // передаём число секунд ожидания
            } catch (InterruptedException e) {
            }
            Log.d("mytag", "Pause finished");
            return null;
        }

        // после паузы, перевернуть все карты обратно
        @Override
        protected void onPostExecute(Void aVoid) {
            for (Card c : cards) {
                if (c.isOpen) {
                    c.isOpen = false;
                }
            }
            openedCard = 0;
            isOnPauseNow = false;
            // если открылись одинаковые карты, удалить их из списка
            checkOpenCardsEqual(firstCard, secondCard);
            invalidate();
        }
    }
}