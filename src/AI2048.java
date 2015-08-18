import java.util.Arrays;
import java.util.HashMap;

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

		int next() {
			long t = (x ^ x << 11);
			x = y;
			y = z;
			z = w;
			w = (w ^ w >>> 19 ^ t ^ t >>> 8);
			return (int) w;
		}

		int nextAbs() {
			return Math.abs(next());
		}

		long nextLong() {
			long t = (x ^ x << 11);
			x = y;
			y = z;
			z = w;
			w = (w ^ w >>> 19 ^ t ^ t >>> 8);
			return w;
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
		int sum = 0, best = 0;
		for (int x = 0; x < n; ++x) {
			state state = new state();
			long start = System.nanoTime();
			while (state.putTile()) {
				Arrow a = solve(Arrays.copyOf(state.board, 16));
				if (a == null) {
					break;
				}
				state.step(a);
			}
			state.print();
			sum += state.score;
			best = Math.max(best, state.score);

			System.out.println("time	: " + (System.nanoTime() - start) / 1000 / 1000);
			System.out.println("score	: " + state.score);
			System.out.println("average	: " + (double) sum / (x + 1));
			System.out.println("best	: " + best);
		}
	}

	static final int n = 4;
	static final int nn = n * n;
	HashMap<Integer, Integer> toA = new HashMap<>();
	long hashMap[][] = new long[nn][nn];

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

	{
		toA.put(0, 0);
		for (int i = 0; i < nn; ++i) {
			toA.put(1 << (i + 1), i + 1);
			for (int j = 0; j < nn; ++j) {
				hashMap[i][j] = random.nextLong();
			}
		}
	}

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
		int board[] = new int[nn];
		{// setTile
			for (WebElement tile : tileContainer.findElements(By.className("tile"))) {
				String classes[] = tile.getAttribute("class").split(" ");
				int value = Integer.parseInt(classes[1].split("-")[1]);
				int x = Integer.parseInt(classes[2].split("-")[2]) - 1;
				int y = Integer.parseInt(classes[2].split("-")[3]) - 1;
				board[y * n + x] = toA.get(value);
			}
		}
		return solve(board);
	}

	static final int map_depth = 4;
	@SuppressWarnings("unchecked")
	HashMap<Long, Integer> memo[] = new HashMap[map_depth + 1];
	{
		for (int i = 0; i <= map_depth; ++i) {
			memo[i] = new HashMap<>();
		}
	}

	Arrow solve(int board[]) {
		Arrow res = null;
		int value = Integer.MIN_VALUE;
		for (int i = 0; i <= map_depth; ++i)
			memo[i].clear();
		for (Arrow arrow : Arrow.values()) {
			state tmp = new state(board);
			if (tmp.step(arrow)) {
				int tmpValue = dfs(tmp, tmp.empty() < 6 ? map_depth : map_depth - 1);
				if (value < tmpValue) {
					value = tmpValue;
					res = arrow;
				}
			}
		}
		return res;
	}

	int dfs(state board, int depth) {
		long key = board.hash();
		Integer x = memo[depth].get(key);
		if (x != null) return x;
		int count = 0, value = 0;
		for (int i = 0; i < nn; ++i) {
			if (board.board[i] == 0) {
				++count;
				state tmp = new state(board);
				tmp.board[i] = 1;
				int tmpValue = Integer.MIN_VALUE >> 8;
				for (Arrow arrow : Arrow.values()) {
					state next = new state(tmp);
					if (next.step(arrow)) tmpValue = Math.max(tmpValue, (depth == 0 ? next.score + next.value() : dfs(next, depth - 1)));
				}
				value += tmpValue;
			}
		}
		value /= count;
		memo[depth].put(key, value);
		return value;
	}

	class state {
		int board[];
		int score;

		public state() {
			board = new int[nn];
		}

		public state(int board[]) {
			this.board = Arrays.copyOf(board, nn);
		}

		public state(state s) {
			board = Arrays.copyOf(s.board, nn);
			score = s.score;
		}

		public boolean putTile() {
			int vi[] = new int[nn];
			int index = 0;
			for (int i = 0; i < nn; ++i) {
				if (board[i] == 0) vi[index++] = i;
			}
			if (index == 0) return false;
			board[vi[random.nextAbs() % index]] = (random.nextAbs() % 10 == 0 ? 2 : 1);
			return true;
		}

		public void print() {
			for (int i = 0; i < n; ++i) {
				for (int j = 0; j < n; ++j) {
					System.out.print(String.format("%3d", board[i * n + j]));
				}
				System.out.println();
			}
			System.out.println();
		}

		public boolean step(Arrow arrow) {
			boolean ok = false;
			if (arrow == Arrow.up) {
				for (int i = 0; i < n; ++i) {
					int index = 0;
					for (int j = n; j < nn; j += n) {
						if (board[i + j] != 0) {
							while (index < j) {
								if (board[index + i] == 0) {
									board[index + i] = board[i + j];
									board[i + j] = 0;
									ok = true;
									break;
								} else if (board[index + i] == board[i + j]) {
									++board[index + i];
									board[i + j] = 0;
									ok = true;
									score += 1 << board[index + i];
									index += 4;
									break;
								}
								index += 4;
							}
						}
					}
				}
			} else if (arrow == Arrow.down) {
				for (int i = 0; i < n; ++i) {
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
									++board[index + i];
									board[i + j] = 0;
									ok = true;
									score += 1 << board[index + i];
									index -= 4;
									break;
								}
								index -= 4;
							}
						}
					}
				}
			} else if (arrow == Arrow.right) {
				for (int i = 0; i < nn; i += n) {
					int index = 3;
					for (int j = 2; j >= 0; --j) {
						if (board[i + j] != 0) {
							while (index > j) {
								if (board[index + i] == 0) {
									board[index + i] = board[i + j];
									board[i + j] = 0;
									ok = true;
									break;
								} else if (board[index + i] == board[i + j]) {
									++board[index + i];
									board[i + j] = 0;
									ok = true;
									score += 1 << board[index + i];
									--index;
									break;
								}
								--index;
							}
						}
					}
				}
			} else if (arrow == Arrow.left) {
				for (int i = 0; i < nn; i += n) {
					int index = 0;
					for (int j = 1; j < n; ++j) {
						if (board[i + j] != 0) {
							while (index < j) {
								if (board[index + i] == 0) {
									board[index + i] = board[i + j];
									board[i + j] = 0;
									ok = true;
									break;
								} else if (board[index + i] == board[i + j]) {
									++board[index + i];
									board[i + j] = 0;
									ok = true;
									score += 1 << board[index + i];
									++index;
									break;
								}
								++index;
							}
						}
					}
				}
			}
			return ok;
		}

		int empty() {
			int res = 0;
			for (int i = 0; i < nn; ++i)
				if (board[i] == 0) ++res;
			return res;
		}

		int value() {
			int value = 0;
			for (int i = 0; i < 3; ++i) {
				value += Math.abs(board[i + 0] - board[i + 0 + 1]);
				value += Math.abs(board[i + 4] - board[i + 4 + 1]);
				value += Math.abs(board[i + 8] - board[i + 8 + 1]);
				value += Math.abs(board[i + 12] - board[i + 12 + 1]);

				value += Math.abs(board[i * 4 + 0] - board[i * 4 + 0 + 4]);
				value += Math.abs(board[i * 4 + 1] - board[i * 4 + 1 + 4]);
				value += Math.abs(board[i * 4 + 2] - board[i * 4 + 2 + 4]);
				value += Math.abs(board[i * 4 + 3] - board[i * 4 + 3 + 4]);
			}
			return -value;
		}

		long hash() {
			return hashMap[0][board[0]] ^ hashMap[1][board[1]] ^ hashMap[2][board[2]] ^ hashMap[3][board[3]] ^ hashMap[4][board[4]]
					^ hashMap[5][board[5]] ^ hashMap[6][board[6]] ^ hashMap[7][board[7]] ^ hashMap[8][board[8]] ^ hashMap[9][board[9]]
					^ hashMap[10][board[10]] ^ hashMap[11][board[11]] ^ hashMap[12][board[12]] ^ hashMap[13][board[13]]
					^ hashMap[14][board[14]] ^ hashMap[15][board[15]];
		}
	}
}
