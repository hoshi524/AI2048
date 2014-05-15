package testSelenium;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class seleniumTest {

	public static void main(String[] args) {
		try {
			new seleniumTest().solve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	enum Arrow {
		down(Keys.ARROW_DOWN, 3, -1, -1), //
		up(Keys.ARROW_UP, 0, 4, 1), //
		right(Keys.ARROW_RIGHT, 3, -1, -1), //
		left(Keys.ARROW_LEFT, 0, 4, 1);

		Keys key;
		final int start, end, ite;

		Arrow(Keys key, int start, int end, int ite) {
			this.key = key;
			this.start = start;
			this.end = end;
			this.ite = ite;
		}
	}

	Random random = new Random();
	WebDriver driver;
	WebElement body, tileContainer;

	public void solve() throws Exception {
		driver = new FirefoxDriver();
		driver.get("http://gabrielecirulli.github.io/2048/");
		body = driver.findElement(By.tagName("body"));
		tileContainer = driver.findElement(By.className("tile-container"));
		while (true) {
			Arrow next = think();
			if (next != null) {
				body.sendKeys(next.key);
				Thread.sleep(30);
			} else {
				break;
			}
		}
		// driver.quit();
	}

	Arrow prev;

	public Arrow think() {
		int board[][] = new int[4][4];
		{// setTile
			for (WebElement tile : tileContainer.findElements(By.className("tile"))) {
				String classes[] = tile.getAttribute("class").split(" ");
				int value = Integer.parseInt(classes[1].split("-")[1]);
				int x = Integer.parseInt(classes[2].split("-")[2]) - 1;
				int y = Integer.parseInt(classes[2].split("-")[3]) - 1;
				board[y][x] = value;
			}
		}
		Arrow res = null;
		int value = Integer.MIN_VALUE;
		for (Arrow arrow : Arrow.values()) {
			int tmp[][] = copy(board);
			//			System.out.println(arrow);
			//			System.out.println("Before");
			//			for (int i = 0; i < 4; i++) {
			//				for (int j = 0; j < 4; j++) {
			//					System.out.print(tmp[i][j] + " ");
			//				}
			//				System.out.println();
			//			}
			if (step(tmp, arrow)) {
				int tmpValue = dfs(tmp, 5);
				if (prev != arrow && tmpValue > value) {
					value = tmpValue;
					res = arrow;
				}
			}
			//			System.out.println("after");
			//			for (int i = 0; i < 4; i++) {
			//				for (int j = 0; j < 4; j++) {
			//					System.out.print(tmp[i][j] + " ");
			//				}
			//				System.out.println();
			//			}
			//			System.out.println();
		}
		prev = res;
		System.out.println(res);
		return res;
	}

	public int dfs(int board[][], int depth) {
		if (depth == 0) {
			return calcValue(board);
		}
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int tmp[][] = copy(board);
			putTile(tmp);
			int tmpValue = Integer.MIN_VALUE;
			for (Arrow arrow : Arrow.values()) {
				int next[][] = copy(tmp);
				if (step(next, arrow)) {
					tmpValue = Math.max(tmpValue, dfs(next, depth - 1));
				}
			}
			value += tmpValue;
		}
		return value;
	}

	private int vx[] = new int[16];
	private int vy[] = new int[16];

	public void putTile(int board[][]) {
		int index = 0;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				if (board[i][j] == 0) {
					vx[index] = i;
					vy[index] = j;
					index++;
				}
			}
		}
		index = random.nextInt(index);
		board[vx[index]][vy[index]] = (random.nextInt(10) == 0 ? 4 : 2);
	}

	public boolean step(int board[][], Arrow arrow) {
		boolean ok = false;
		if (arrow == Arrow.up || arrow == Arrow.down) {
			for (int i = 0; i < 4; i++) {
				int index = arrow.start;
				for (int j = arrow.start + arrow.ite; j != arrow.end; j += arrow.ite) {
					if (board[j][i] != 0) {
						if (board[index][i] == 0 || board[index][i] == board[j][i]) {
							board[index][i] += board[j][i];
							board[j][i] = 0;
							ok = true;
						}
						index += arrow.ite;
					}
				}
			}
		} else {
			// right left
			for (int i = 0; i < 4; i++) {
				int index = arrow.start;
				for (int j = arrow.start + arrow.ite; j != arrow.end; j += arrow.ite) {
					if (board[i][j] != 0) {
						if (board[i][index] == 0 || board[i][index] == board[i][j]) {
							board[i][index] += board[i][j];
							board[i][j] = 0;
							ok = true;
						}
						index += arrow.ite;
					}
				}
			}
		}
		return ok;
	}

	public int[][] copy(int board[][]) {
		int tmp[][] = new int[board.length][];
		for (int i = 0; i < board.length; i++) {
			tmp[i] = Arrays.copyOf(board[i], board[i].length);
		}
		return tmp;
	}

	public int calcValue(int board[][]) {
		int value = 0;
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int[] buf : board) {
			for (int num : buf) {
				if (num == 0) {
					value += 16;
				} else {
					Integer count = map.get(num);
					map.put(num, (count == null ? 0 : count) + 1);
				}
			}
		}
		for (Entry<Integer, Integer> entry : map.entrySet()) {
			value += entry.getKey() * (2 - entry.getValue());
		}
		return value;
	}
}
