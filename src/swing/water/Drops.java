/* Formulae from WM_Water.java by http://www.valarsoft.com Webmatic Applets
 *
 * Copyright (c) 2014, Leo.Kuznetsov@gmail.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package swing.water;

import javax.imageio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

import static java.awt.EventQueue.invokeLater;

public class Drops {

    private static final int
            FPS = 24;
    private static final int
            RIPPLE_LEFT   = 0x01,
            RIPPLE_TOP    = 0x02,
            RIPPLE_RIGHT  = 0x04,
            RIPPLE_BOTTOM = 0x08,
            DROPS         = 0x10,
            SKIMMER       = 0x20, // Gerridae
            MOUSE_DROPS   = 0x40;

    private static View view;
    private static BufferedImage tile;
    private static BufferedImage image;
    private static final int[] F = new int[720];

    static {
//      https://www.google.com/search?q=tan(asin((sin(atan(x))/4)))
        for (int i = -360; i < 360; i++) {
            F[i + 360] = (int)((Math.tan(Math.asin((Math.sin(Math.atan(i)) / 4))) * i));
        }
    }

    public static void main(String[] args) {
        invokeLater(new Runnable() {
            public void run() {
                try {
                    tile = ImageIO.read(Drops.class.getResourceAsStream("background396x396.jpg"));
                } catch (IOException e) {
                    throw new Error(e);
                }
                JFrame frame = new JFrame();
                frame.setSize(tile.getWidth(), tile.getHeight());
                frame.setVisible(true);
                view = new View();
                view.setMode(RIPPLE_LEFT| RIPPLE_TOP | RIPPLE_RIGHT | RIPPLE_BOTTOM| DROPS | SKIMMER | MOUSE_DROPS);
                view.setSize(tile.getWidth(), tile.getHeight());
                frame.setContentPane(view);
            }
        });
    }

    private static class View extends Container {
        int w;
        int h;
        int pixels[];
        BufferedImage dest;
        int[][] map;
        int[] img;
        int mode;
        boolean flip = true;
        int WATER = 800;
        int SEED = 2;

        int currentX = 1, currentY = 1, dx = 1, dy = 1; // Surfing

        View() {
            addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    onResize();
                    super.componentResized(e);
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    onMouseMoved(e, e.getX(), e.getY());
                    super.mouseMoved(e);
                }
            });
            Timer timer = new Timer(1000 / FPS, new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    repaint();
                }
            });
            timer.setRepeats(true);
            timer.start();
        }

        public void setMode(int m) {
            mode = m;
        }

        public void onMouseMoved(MouseEvent e, int x, int y) {
            int next = flip ? 1 : 0;
            int current = 1 - next;
            if ((mode & MOUSE_DROPS) != 0 && 1 < x && x < w - 1 && 1 < y && y < h - 1) {
                map[current][x + y * w] = rand(WATER);
            }
        }

        void onResize() {
            w = getWidth();
            h = getHeight();
            if (w == 0 || h == 0) {
                return;
            }
            image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = image.createGraphics();
            for (int y = 0; y < h; y += tile.getHeight()) {
                for (int x = 0; x < w; x += tile.getWidth()) {
                    g.drawImage(tile, x, y, null);
                }
            }
            g.dispose();
            currentX = 1;
            currentY = 1;
            dx = 1;
            dy = 1;
            dest = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            pixels = image.getRGB(0, 0, w, h, null, 0, w);
            img = new int[w * h];
            System.arraycopy(pixels, 0, img, 0, pixels.length);
            map = new int[2][w * h];
            wave1();
            wave2();
        }

        public void paint(Graphics g) {
            if (img == null || img.length != w * h) {
                return;
            }
            wave0();
            water();
            dest.setRGB(0, 0, w, h, img, 0, w);
            g.drawImage(dest, 0, 0, null);
            flip = !flip;
            wave2();
        }

        public static int rand(int max) {
            return (int)(Math.random() * (max + 1));
        }

        public void wave0() {
            int[] next = map[flip ? 1 : 0];
            int[] current = map[flip ? 0 : 1];
            for (int x = 1; x < w - 1; x++) {
                for (int y = 1; y < h - 1; y++) {
                    int c = ((current[(x + 1) + y * w] +
                              current[(x - 1) + y * w] +
                              current[x + (y + 1) * w] +
                              current[x + (y - 1) * w]) >> 1) - next[x + y * w];
                    next[x + y * w] = c - (c >> 4);
                }
            }
        }

        public void wave1() {
            int[] next = map[flip ? 1 : 0];
            int[] current = map[flip ? 0 : 1];
            for (int x = 1; x < w - 1; x++) {
                for (int y = 1; y < h - 1; y++) {
                    current[x + y * w] = 0;
                    next[x + y * w] = 0;
                }
            }
        }

        public void wave2() {
            int[] current = map[flip ? 0 : 1];
            if ((mode & RIPPLE_LEFT) != 0) {
                for (int y = 1; y <= h - 2; y += 10) {
                    current[2 + y * w] = rand(WATER);
                }
            }
            if ((mode & RIPPLE_TOP) != 0) {
                for (int x = 1; x <= w - 2; x += 10) {
                    current[x + 2 * w] = rand(WATER);
                }
            }
            if ((mode & RIPPLE_RIGHT) != 0) {
                for (int y = 1; y <= h - 2; y += 10) {
                    current[(w - 2) + y * w] = rand(WATER);
                }
            }
            if ((mode & RIPPLE_BOTTOM) != 0) {
                for (int x = 1; x <= w - 2; x += 10) {
                    current[x + (h - 2) * w] = rand(WATER);
                }
            }
            if ((mode & DROPS) != 0) {
                if (rand(SEED) == 0) {
                    current[rand(w - 1) + rand(h - 1) * w] = rand(WATER);
                }
            }
            // Gerridae - Water Skimmer trail
            if ((mode & SKIMMER) != 0) {
                final int speed = 2;
                current[currentX + currentY * w] = rand(WATER);
                currentX += dx;
                if (currentX > w - 2 * speed) {
                    dx = -speed;
                }
                if (currentX <= speed) {
                    dx = speed;
                }
                currentY += dy;
                if (currentY > h - speed * 2) {
                    dy = -speed;
                }
                if (currentY <= speed) {
                    dy = speed;
                }
            }
        }

        public void water() {
            int[] next = map[flip ? 1 : 0];
            for (int x = 1; x < w - 1; x++) {
                for (int y = 1; y < h - 1; y++) {
                    int diffX = Math.max(-360, Math.min(359, next[(x + 1) + y * w] - next[x + y * w]));
                    int distanceX = F[diffX + 360];
                    int diffY = Math.max(-360, Math.min(359, next[x + (y + 1) * w] - next[x + y * w]));
                    int distanceY = F[diffY + 360];
                    int x1 = Math.min(w - 1, Math.max(0, x - distanceX));
                    int y1 = Math.min(h - 1, Math.max(0, y - distanceY));
                    int x2 = Math.min(w - 1, Math.max(0, x + distanceX));
                    int y2 = Math.min(h - 1, Math.max(0, y + distanceY));
                    int c;
                    if (diffX < 0)  {
                        c = diffY < 0 ? pixels[x1 + y1 * w] : pixels[x1 + y2 * w];
                    } else {
                        c = diffY < 0 ? pixels[x2 + y1 * w] : pixels[x2 + y2 * w];
                    }
                    img[x + y * w] = c;
                }
            }
        }

    }

}
