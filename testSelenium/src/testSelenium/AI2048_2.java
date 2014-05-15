package testSelenium;

import java.util.Arrays;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class AI2048_2 {

	public static void main(String[] args) {
		try {
			new AI2048_2().solve();
		} catch (Exception e) {
			e.printStackTrace();
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

	Random random = new Random();
	WebDriver driver;
	WebElement body, tileContainer, keep;

	public void solve() throws Exception {
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
				Thread.sleep(50);
			} else {
				break;
			}
		}
		// driver.quit();
	}

	public Arrow think() {
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
		Arrow res = null;
		int value = Integer.MIN_VALUE;
		for (Arrow arrow : Arrow.values()) {
			state tmp = new state(board);
			if (tmp.step(arrow)) {
				int tmpValue = dfs(tmp, 4);
				if (tmpValue > value) {
					value = tmpValue;
					res = arrow;
				}
			}
		}
		System.out.println(res);
		return res;
	}

	public int dfs(state board, int depth) {
		int value = 0;
		for (int i = 0; i < 3; i++) {
			state tmp = new state(board);
			tmp.putTile();
			int tmpValue = 0;
			for (Arrow arrow : Arrow.values()) {
				state next = new state(tmp);
				if (next.step(arrow)) {
					tmpValue = Math.max(tmpValue, (depth == 0 ? next.value : dfs(next, depth - 1)));
				}
			}
			value += tmpValue;
		}
		return value;
	}

	class state {
		int board[];
		int value;

		public state(int board[]) {
			this.board = Arrays.copyOf(board, 16);
			value = 0;
		}

		public state(state s) {
			board = Arrays.copyOf(s.board, 16);
			value = s.value;
		}

		public void putTile() {
			int vi[] = new int[16];
			int index = 0;
			for (int i = 0; i < 16; i++) {
				if (board[i] == 0) {
					vi[index] = i;
					index++;
				}
			}
			index = random.nextInt(index);
			board[vi[index]] = (random.nextInt(10) == 0 ? 4 : 2);
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
									board[index + i] += board[i + j];
									board[i + j] = 0;
									ok = true;
									value += board[index + i] * board[index + i];
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
									board[index + i] += board[i + j];
									board[i + j] = 0;
									ok = true;
									value += board[index + i] * board[index + i];
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
									board[index + i] += board[i + j];
									board[i + j] = 0;
									ok = true;
									value += board[index + i] * board[index + i];
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
									board[index + i] += board[i + j];
									board[i + j] = 0;
									ok = true;
									value += board[index + i] * board[index + i];
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
