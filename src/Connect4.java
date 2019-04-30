import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Connect4 {
    //default board size
    public final static int ROWS = 4;
    public final static int COLS = 5;
    //decide if you want connect 3 or 4
    public final static int CONNECT_NUM = 4;

    public static HashMap<GameState, Integer> memo = new HashMap();

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        String filename = args[0];
        File f = new File(filename);
        GameState initState = getInitialState(f);

        //gets the next move with optimal utility
        if(initState.isGoalNode()){
            System.out.println("Congratulations, Player "+ initState.lastPlayer + " won!");
        }
        else if(initState.isTie()){
            System.out.println("The game ended in a tie.");
        }
        else {
            GameState bestMove = getBestMove(initState, Integer.MIN_VALUE, Integer.MAX_VALUE);
            System.out.println("It is Player "+initState.currPlayer+"\'s turn");
            String outcome = "";

            switch(bestMove.utility){
                case -1:
                    outcome = "loss";
                    break;
                case 0:
                    outcome = "tie";
                    break;
                case 1:
                    outcome = "win";
                    break;
                default:
                    break;
            }
            long endTime = System.currentTimeMillis();
            System.out.println("The worst case outcome for player X is a "+outcome+" with utility = "+bestMove.utility);
            System.out.println("You should move to column "+(bestMove.lastMove+1));
            System.out.println("Time elapsed : " + (endTime - startTime) + " ms");
        }
    }

    static GameState getInitialState(File file) {
        String[][] state = new String[ROWS][COLS];
        try {
            Scanner sc = new Scanner(file);
            int i = 0;
            while (sc.hasNextLine()) {
                state[i++] = sc.nextLine().split("");
            }
            sc.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        char[][] charSt = new char[ROWS][COLS];

        for(int i = 0; i < state.length; i++){
            for(int j = 0; j < state[0].length; j++){
                charSt[i][j] = state[i][j].charAt(0);
            }
        }

        return new GameState(charSt);
    }

    static GameState getBestMove(GameState node, int alpha, int beta) {
        //base case when it reaches a goal node or the game ends in a tie
        if(memo.containsKey(node)){
            node.utility = memo.get(node);
            return node;
        }

        if(node.isGoalNode()) {
            if(node.lastPlayer == 'X') {
                node.utility = 1;
            }
            else {
                node.utility = -1;
            }
            memo.put(node, node.utility);
            return node;
        }

        else if(node.isTie()) {
            node.utility = 0;
            memo.put(node, node.utility);
            return node;
        }

        else {
            GameState bestMove = null;
            //adds children to the node in the tree
            ArrayList<GameState> children = new ArrayList<>();
            for(int i = 0; i < COLS; i++) {
                if(node.childPossible(i)) {
                    children.add(node.makeChild(i));
                }
            }

            Collections.sort(children, new GameStateComparator());

            for(int j = 0; j < children.size(); j++){
                GameState childNode = children.get(j);
                int childUtility = getBestMove(childNode, alpha, beta).utility;
                if(node.currPlayer == 'X' && childUtility > node.utility) {
                    node.utility = childUtility;
                    bestMove = childNode;

                    // pruning
                    alpha = Math.max(alpha, node.utility);
                    if(beta <= alpha) {
                        break;
                    }
                }
                else if (node.currPlayer == 'O' && childUtility < node.utility) {
                    node.utility = childUtility;
                    bestMove = childNode;

                    // pruning
                    beta = Math.min(beta, node.utility);
                    if(beta <= alpha) {
                        break;
                    }
                }
            }

            memo.put(node, node.utility);
            return bestMove;
        }
    }
}

class GameState{
    char[][] board;
    char currPlayer;
    char lastPlayer;
    int utility;
    int lastMove;

    public GameState(char[][] board) {
        this.board = board;
        this.currPlayer = getPlayer();
        this.lastPlayer = (currPlayer == 'X') ? 'O' : 'X';
        this.utility = (currPlayer == 'X') ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }

    public GameState(char[][] board, char player, int move) {
        this.board = board;
        this.lastMove = move;
        this.currPlayer = player;
        this.lastPlayer = (currPlayer == 'X') ? 'O' : 'X';
        this.utility = (currPlayer == 'X') ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object obj) {
        GameState other = (GameState)obj;
        return this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    //called initially to calculate whose turn it is
    public char getPlayer() {
        int xCount = 0, oCount = 0;
        for(int i = 0; i < board.length; i++) {
            for(int j = 0; j < board[i].length; j++) {
                if(board[i][j] == 'X')
                    xCount++;
                else if(board[i][j] == 'O')
                    oCount++;
            }
        }
        if (xCount > oCount)
            return 'O';
        else return 'X';
    }

    public boolean isGoalNode(){
        return getMaxInARow() == Connect4.CONNECT_NUM;
    }

    public int getMaxInARow() {
        int maxInARow = 0;
        for(int i = 0; i < board.length; i++) {
            for(int j = 0; j < board[0].length; j++) {
                char player = board[i][j];
                //ignore slots that aren't relevant to our check
                if(player != lastPlayer) {
                    continue;
                }

                //checks row
                int inARow = 1;
                for(int k = 1; k < Connect4.CONNECT_NUM; k++){
                    if((j + k < board[0].length) && player == board[i][j+k]){
                        inARow++;
                    }
                    else break;
                }
                maxInARow = Math.max(maxInARow, inARow);


                //checks column
                inARow = 1;
                for(int k = 1; k < Connect4.CONNECT_NUM; k++){
                    if((i + k < board.length) && player == board[i+k][j]){
                        inARow++;
                    }
                    else break;
                }
                maxInARow = Math.max(maxInARow, inARow);

                //checks bottom right diag
                inARow = 1;
                for(int k = 1; k < Connect4.CONNECT_NUM; k++){
                    if((i + k < board.length) && (j + k < board[0].length) &&  player == board[i+k][j+k]){
                        inARow++;
                    }
                    else break;
                }
                maxInARow = Math.max(maxInARow, inARow);

                //checks bottom left diag
                inARow = 1;
                for(int k = 1; k < Connect4.CONNECT_NUM; k++){
                    if((i + k < board.length) && (j - k >= 0) && player == board[i+k][j-k]){
                        inARow++;
                    }
                    else break;
                }
                maxInARow = Math.max(maxInARow, inARow);
            }
        }

        return maxInARow;
    }

    //called after isGoalNode
    public boolean isTie() {
        for(int i = 0; i < board.length; i++) {
            for(int j = 0; j < board[0].length; j++) {
                if(board[i][j] == '.'){
                    return false;
                }
            }
        }
        return true;
    }

    //inserts a player's token in a given column by moving down the column till it reaches the last empty slot
    public GameState makeChild(int column) {
        char[][] newState = new char[board.length][board[0].length];
        //deep copy of the board
        for(int i = 0; i < board.length; i++) {
            for(int j = 0; j < board[0].length; j++) {
                newState[i][j] = board[i][j];
            }
        }
        for(int i = 1; i < newState.length; i++) {
            //first non-empty slot
            if(board[i][column] != '.') {
                newState[i-1][column] = currPlayer;
                break;
            }
            //if the whole column is empty
            if(i == newState.length - 1) {
                newState[i][column] = currPlayer;
                break;
            }
        }
        return new GameState(newState, lastPlayer, column);
    }

    //checks if column is full to decide if you can insert to that column
    //called before makeChild
    public boolean childPossible(int column) {
        return board[0][column] == '.';
    }

    //added for debugging
    public String toString() {
        String ret = "";
        for(char[] row: board) {
            for(char i: row) {
                ret += (i+" ");
            }
            ret += "\n";
        }
        return ret;
    }
}

class GameStateComparator implements Comparator<GameState>
{
    @Override
    public int compare(GameState x, GameState y)
    {
        if (x.getMaxInARow() > y.getMaxInARow())
        {
            return -1;
        }
        if (x.getMaxInARow() < y.getMaxInARow())
        {
            return 1;
        }
        return 0;
    }
}