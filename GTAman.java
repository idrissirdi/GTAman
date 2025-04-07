import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

public class GTAman extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            int speed = (speedBoostActive) ? tileSize / 6 : tileSize / 4; // Faster speed during boost
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -speed;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = speed;
            }
            else if (this.direction == 'L') {
                this.velocityX = -speed;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = speed;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    private Image cherryImage;
    private Image cherry2Image;
    
    private Image powerFoodImage;
    private Image scaredGhostImage;

    private Block cherry; // For storing the cherry block
    private boolean speedBoostActive = false; // To track the speed boost
    private long speedBoostStartTime; // To track the start time of the speed boost
    private Timer cherryTimer; // To respawn the cherry every 1 minute

    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red
    private String[] tileMap = {
    "XXXXXXXXXXXXXXXXXXX",
    "X        X        X",
    "X XX XXX X XXX XX X",
    "X                 X",
    "X XX X XXXXX X XX X",
    "X    X       X    X",
    "XXXX XXXX XXXX XXXX",
    "OOOX X       X XOOO",
    "XXXX X XXrXX X XXXX",
    "O       bpo       O",
    "XXXX X XXXXX X XXXX",
    "OOOX X       X XOOO",
    "XXXX X XXXXX X XXXX",
    "X        X        X",
    "X XX XXX X XXX XX X",
    "X  X     P     X  X",
    "XX X X XXXXX X X XX",
    "X    X   X   X    X",
    "X XXXXXX X XXXXXX X",
    "X                 X",
    "XXXXXXXXXXXXXXXXXXX" 
};


    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;

    GTAman() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
        
        cherryImage = new ImageIcon(getClass().getResource("./cherry.png")).getImage();
        cherry2Image = new ImageIcon(getClass().getResource("./cherry2.png")).getImage();
        powerFoodImage = new ImageIcon(getClass().getResource("./powerFood.png")).getImage();
        scaredGhostImage = new ImageIcon(getClass().getResource("./scaredGhost.png")).getImage();


        loadMap();
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }

        gameLoop = new Timer(50, this); // 20fps
        gameLoop.start();

        // Timer to respawn cherry every 1 minute (60,000 ms)
        cherryTimer = new Timer(60000, e -> spawnCherry());
        cherryTimer.start();
    }

    public void loadMap() {
    walls = new HashSet<Block>();
    foods = new HashSet<Block>();  // To hold the pellet objects
    ghosts = new HashSet<Block>();

    for (int r = 0; r < rowCount; r++) {
        for (int c = 0; c < columnCount; c++) {
            String row = tileMap[r];
            char tileMapChar = row.charAt(c);

            int x = c * tileSize;
            int y = r * tileSize;

            if (tileMapChar == 'X') { // wall
                Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                walls.add(wall);
            }
            else if (tileMapChar == 'b') { // blue ghost
                Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                ghosts.add(ghost);
            }
            else if (tileMapChar == 'o') { // orange ghost
                Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                ghosts.add(ghost);
            }
            else if (tileMapChar == 'p') { // pink ghost
                Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                ghosts.add(ghost);
            }
            else if (tileMapChar == 'r') { // red ghost
                Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                ghosts.add(ghost);
            }
            else if (tileMapChar == 'P') { // pacman
                pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
            }
            else if (tileMapChar == ' ') { // empty space (food - pellet)
                Block pellet = new Block(null, x + 14, y + 14, 4, 4); // Pellets are small, so we size them to 4x4
                foods.add(pellet);
            }
            else if (tileMapChar == 'O') { // power food
                Block powerFood = new Block(powerFoodImage, x + 14, y + 14, 8, 8); // Power food can be slightly bigger
                foods.add(powerFood);
            }
        }
    }
}


    public void spawnCherry() {
        // Pick a random empty space
        boolean spawned = false;
        while (!spawned) {
            int r = random.nextInt(rowCount);
            int c = random.nextInt(columnCount);
            String row = tileMap[r];
            char tileMapChar = row.charAt(c);
            
            if (tileMapChar == ' ' || tileMapChar == 'O') { // Empty space
                int x = c * tileSize;
                int y = r * tileSize;
                cherry = new Block(cherryImage, x + 14, y + 14, 4, 4); // Create cherry at random location
                spawned = true;
            }
        }
    }

    public void move() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Check wall collisions
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        // Check ghost collisions
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                lives -= 1;
                if (lives == 0) {
                    gameOver = true;
                    return;
                }
                resetPositions();
            }

            if (ghost.y == tileSize * 9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }
        }

        // Check for cherry collision
        if (cherry != null && collision(pacman, cherry)) {
            score += 100; // Grant points
            speedBoostActive = true; // Activate speed boost
            speedBoostStartTime = System.currentTimeMillis(); // Track start time of speed boost
            cherry = null; // Remove the cherry
            spawnCherry(); // Spawn a new cherry
        }

        // Check food collision (existing code)
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
            }
        }
        foods.remove(foodEaten);

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }

        // Handle speed boost duration (5 seconds)
        if (speedBoostActive && System.currentTimeMillis() - speedBoostStartTime >= 5000) {
            speedBoostActive = false; // Deactivate speed boost after 5 seconds
        }
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        for (Block ghost : ghosts) {
            ghost.reset();
        }
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }

        // Display the cherry indicator if collected
        if (cherry == null) {
            g.drawImage(cherry2Image, tileSize * 7, tileSize / 2, 20, 20, null); // Show cherry2 at top
        }

        // Score and lives display
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf(score), tileSize / 2, tileSize / 2);
        } else {
            g.drawString("x" + String.valueOf(lives) + " Score: " + String.valueOf(score), tileSize / 2, tileSize / 2);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            move();
        }
        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP) {
            pacman.updateDirection('U');
        } else if (key == KeyEvent.VK_DOWN) {
            pacman.updateDirection('D');
        } else if (key == KeyEvent.VK_LEFT) {
            pacman.updateDirection('L');
        } else if (key == KeyEvent.VK_RIGHT) {
            pacman.updateDirection('R');
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
    JFrame frame = new JFrame("GTA-MAN");
    GTAman game = new GTAman(); // Initializes the game
    frame.add(game); // Adds the game JPanel to the frame
    frame.pack(); // Fits the frame to the content size
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Closes the app when the window is closed
    frame.setVisible(true); // Ensures the frame is visible
}
@Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g); // clears the background
    if (pacman != null) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }

        if (cherry == null) {
            g.drawImage(cherry2Image, tileSize * 7, tileSize / 2, 20, 20, null);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + score, tileSize / 2, tileSize / 2);
        } else {
            g.drawString("x" + lives + " Score: " + score, tileSize / 2, tileSize / 2);
        }
    }
}

}
