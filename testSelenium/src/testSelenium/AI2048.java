package testSelenium;

import java.util.Arrays;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class AI2048 {

	static final boolean gui = false;

	class XorShift {
		private long x, y, z, w;

		{
			x = 123456789;
			y = 362436069;
			z = 521288629;
			w = 88675123;
		}

		protected int next() {
			long t = (x ^ x << 11);
			x = y;
			y = z;
			z = w;
			w = (w ^ w >>> 19 ^ t ^ t >>> 8);
			return (int) w & ((1 << 31) - 1);
		}
	}

	public static void main(String[] args) {
		if (gui) {
			try {
				new AI2048().solve();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			new AI2048().test();
		}
	}

	void test() {
		final int n = 100;
		int sum = 0;
		for (int x = 0; x < n; x++) {
			state state = new state();
			while (state.putTile()) {
				Arrow a = solve(Arrays.copyOf(state.board, 16));
				if (a == null) {
					break;
				}
				state.step(a);
			}
			state.print();
			System.out.println("score : " + state.score);

			sum += state.score;
			System.out.println("average : " + (double) sum / (x + 1));
		}
	}

	enum Arrow {
		down(Keys.ARROW_DOWN), //
		up(Keys.ARROW_UP), //
		right(Keys.ARROW_RIGHT), //
		left(Keys.ARROW_LEFT);

		Keys key;

		Arrow(Keys key) {
			this.key = key;
		}
	}

	XorShift random = new XorShift();
	WebDriver driver;
	WebElement body, tileContainer, keep;

	void solve() throws Exception {
		driver = new FirefoxDriver();
		driver.get("http://gabrielecirulli.github.io/2048/");
		body = driver.findElement(By.tagName("body"));
		tileContainer = driver.findElement(By.className("tile-container"));
		keep = driver.findElement(By.className("keep-playing-button"));
		while (true) {
			if (keep.isDisplayed()) {
				keep.click();
			}
			Arrow next = think();
			if (next != null) {
				body.sendKeys(next.key);
			} else {
				break;
			}
		}
		// driver.quit();
	}

	Arrow think() {
		int board[] = new int[16];
		{// setTile
			for (WebElement tile : tileContainer.findElements(By.className("tile"))) {
				String classes[] = tile.getAttribute("class").split(" ");
				int value = Integer.parseInt(classes[1].split("-")[1]);
				int x = Integer.parseInt(classes[2].split("-")[2]) - 1;
				int y = Integer.parseInt(classes[2].split("-")[3]) - 1;
				board[y * 4 + x] = value;
			}
		}
		return solve(board);
	}

	Arrow solve(int board[]) {
		Arrow res = null;
		int value = Integer.MIN_VALUE;
		for (Arrow arrow : Arrow.values()) {
			state tmp = new state(board);
			if (tmp.step(arrow)) {
				int tmpValue = dfs(tmp, 3);
				if (tmpValue > value) {
					value = tmpValue;
					res = arrow;
				}
			}
		}
		return res;
	}

	int dfs(state board, int depth) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			state tmp = new state(board);
			if (tmp.putTile()) {
				int tmpValue = 0;
				for (Arrow arrow : Arrow.values()) {
					state next = new state(tmp);
					if (next.step(arrow)) {
						tmpValue = Math.max(tmpValue, (depth == 0 ? next.score : dfs(next, depth - 1)));
					}
				}
				value += tmpValue;
			}
		}
		return value;
	}

	class state {
		int board[];
		int score = 0;

		public state() {
			board = new int[16];
		}

		public state(int board[]) {
			this.board = Arrays.copyOf(board, 16);
		}

		public state(state s) {
			board = Arrays.copyOf(s.board, 16);
			score = s.score;
		}

		public boolean putTile() {
			int vi[] = new int[16];
			int index = 0;
			for (int i = 0; i < 16; i++) {
				if (board[i] == 0) {
					vi[index] = i;
					index++;
				}
			}
			if (index == 0)
				return false;
			index = random.next() % index;
			board[vi[index]] = (random.next() % 10 == 0 ? 4 : 2);
			return true;
		}

		public void print() {
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					System.out.print(String.format("%5d", board[i * 4 + j]));
				}
				System.out.println();
			}
			System.out.println();
		}

		public boolean step(Arrow arrow) {
			boolean ok = false;
			if (arrow == Arrow.up) {
				for (int i = 0; i < 4; i++) {
					int index = 0;
					for (int j = 4; j < 16; j += 4) {
						if (board[i + j] != 0) {
							while (index < j) {
								if (board[index + i] == 0) {
									board[index + i] = board[i + j];
									board[i + j] = 0;
									ok = true;
									break;
								} else if (board[index + i] == board[i + j]) {
									board[index + i] <<= 1;
									board[i + j] = 0;
									ok = true;
									score += board[index + i];
									index += 4;
									break;
								}
								index += 4;
							}
						}
					}
				}
			} else if (arrow == Arrow.down) {
				for (int i = 0; i < 4; i++) {
					int index = 12;
					for (int j = 8; j >= 0; j -= 4) {
						if (board[i + j] != 0) {
							while (index > j) {
								if (board[index + i] == 0) {
									board[index + i] = board[i + j];
									board[i + j] = 0;
									ok = true;
									break;
								} else if (board[index + i] == board[i + j]) {
									board[index + i] <<= 1;
									board[i + j] = 0;
									ok = true;
									score += board[index + i];
									index -= 4;
									break;
								}
								index -= 4;
							}
						}
					}
				}
			} else if (arrow == Arrow.right) {
				for (int i = 0; i < 16; i += 4) {
					int index = 3;
					for (int j = 2; j >= 0; j--) {
						if (board[i + j] != 0) {
							while (index > j) {
								if (board[index + i] == 0) {
									board[index + i] = board[i + j];
									board[i + j] = 0;
									ok = true;
									break;
								} else if (board[index + i] == board[i + j]) {
									board[index + i] <<= 1;
									board[i + j] = 0;
									ok = true;
									score += board[index + i];
									index--;
									break;
								}
								index--;
							}
						}
					}
				}
			} else if (arrow == Arrow.left) {
				for (int i = 0; i < 16; i += 4) {
					int index = 0;
					for (int j = 1; j < 4; j++) {
						if (board[i + j] != 0) {
							while (index < j) {
								if (board[index + i] == 0) {
									board[index + i] = board[i + j];
									board[i + j] = 0;
									ok = true;
									break;
								} else if (board[index + i] == board[i + j]) {
									board[index + i] <<= 1;
									board[i + j] = 0;
									ok = true;
									score += board[index + i];
									index++;
									break;
								}
								index++;
							}
						}
					}
				}
			}
			return ok;
		}
	}
}
