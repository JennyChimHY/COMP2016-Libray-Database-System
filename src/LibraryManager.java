import java.awt.GridLayout;
import java.awt.TextField;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;

import java.util.Properties;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.time.*;
import java.time.format.DateTimeFormatter;


/**
 * This is a library manager to support: (1) search for a book (2) borrow a book
 * (3) return a book (4) renew a book (5) reserve a book
 *
 * @author Group 8
 */

public class LibraryManager {

    Scanner in = null;
    Connection conn = null;
    // Database Host
    final String databaseHost = "orasrv1.comp.hkbu.edu.hk";
    // Database Port
    final int databasePort = 1521;
    // Database name
    final String database = "pdborcl.orasrv1.comp.hkbu.edu.hk";
    final String proxyHost = "faith.comp.hkbu.edu.hk";
    final int proxyPort = 22;
    final String forwardHost = "localhost";
    int forwardPort;
    Session proxySession = null;
    boolean noException = true;

    // JDBC connecting host
    String jdbcHost;
    // JDBC connecting port
    int jdbcPort;

    String[] options = { // if you want to add an option, append to the end of
            // this array
            "search for a book", "borrow a book", "return a book",
            "renew a book", "reserve a book",
            "exit"};

    /**
     * Get YES or NO. Do not change this function.
     *
     * @return boolean
     */
    boolean getYESorNO(String message) {
        JPanel panel = new JPanel();
        panel.add(new JLabel(message));
        JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = pane.createDialog(null, "Question");
        dialog.setVisible(true);
        boolean result = JOptionPane.YES_OPTION == (int) pane.getValue();
        dialog.dispose();
        return result;
    }

    /**
     * Get username & password. Do not change this function.
     *
     * @return username & password
     */
    String[] getUsernamePassword(String title) {
        JPanel panel = new JPanel();
        final TextField usernameField = new TextField();
        final JPasswordField passwordField = new JPasswordField();
        panel.setLayout(new GridLayout(2, 2));
        panel.add(new JLabel("Username"));
        panel.add(usernameField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);
        JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
            private static final long serialVersionUID = 1L;

            @Override
            public void selectInitialValue() {
                usernameField.requestFocusInWindow();
            }
        };
        JDialog dialog = pane.createDialog(null, title);
        dialog.setVisible(true);
        dialog.dispose();
        return new String[]{usernameField.getText(), new String(passwordField.getPassword())};
    }

    /**
     * Login the proxy. Do not change this function.
     *
     * @return boolean
     */
    public boolean loginProxy() {
        if (getYESorNO("Using ssh tunnel or not?")) { // if using ssh tunnel
            String[] namePwd = getUsernamePassword("Login cs lab computer");
            String sshUser = namePwd[0];
            String sshPwd = namePwd[1];
            try {
                proxySession = new JSch().getSession(sshUser, proxyHost, proxyPort);
                proxySession.setPassword(sshPwd);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                proxySession.setConfig(config);
                proxySession.connect();
                proxySession.setPortForwardingL(forwardHost, 0, databaseHost, databasePort);
                forwardPort = Integer.parseInt(proxySession.getPortForwardingL()[0].split(":")[0]);
            } catch (JSchException e) {
                e.printStackTrace();
                return false;
            }
            jdbcHost = forwardHost;
            jdbcPort = forwardPort;
        } else {
            jdbcHost = databaseHost;
            jdbcPort = databasePort;
        }
        return true;
    }

    /**
     * Login the oracle system. Change this function under instruction.
     *
     * @return boolean
     */
    public boolean loginDB() {
        String username = "e1234567";//Replace e1234567 to your username
        String password = "e1234567";//Replace e1234567 to your password

        /* Do not change the code below */
        if (username.equalsIgnoreCase("e1234567") || password.equalsIgnoreCase("e1234567")) {
            String[] namePwd = getUsernamePassword("Login sqlplus");
            username = namePwd[0];
            password = namePwd[1];
        }
        String URL = "jdbc:oracle:thin:@" + jdbcHost + ":" + jdbcPort + "/" + database;

        try {
            System.out.println("Logging " + URL + " ...");
            conn = DriverManager.getConnection(URL, username, password);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Show the options. If you want to add one more option, put into the
     * options array above.
     */
    public void showOptions() {
        System.out.println();
        System.out.println("=====================================================");
        System.out.println("Please choose following option:");
        for (int i = 0; i < options.length; ++i) {
            System.out.println("(" + (i + 1) + ") " + options[i]);
        }
        System.out.println("=====================================================");
    }

    /**
     * Run the manager
     */
    public void run() {
        while (noException) {
            showOptions();
            String line = in.nextLine();
            if (line.equalsIgnoreCase("exit"))
                return;
            int choice = -1;
            try {
                choice = Integer.parseInt(line);
            } catch (Exception e) {
                System.out.println("This option is not available");
                continue;
            }
            if (!(choice >= 1 && choice <= options.length)) {
                System.out.println("This option is not available");
                continue;
            }
            if (options[choice - 1].equals("search for a book")) {
                searchBook();
            } else if (options[choice - 1].equals("borrow a book")) {
                borrow();
            } else if (options[choice - 1].equals("return a book")) {
                returnBooks();
            } else if (options[choice - 1].equals("renew a book")) {
                renewBook();
            } else if (options[choice - 1].equals("reserve a book")) {
                reserveBook();
            } else if (options[choice - 1].equals("exit")) {
                break;
            }
        }
    }

    /**
     * Print out the information of a book info
     * This function will select all data from Book table with the specific ISBN
     *
     * @param ISBN
     */
    private void printBookInfo(String ISBN) {
        try {
            Statement stm = conn.createStatement();
            String sql = "select * from books where isbn = " + "'" + ISBN + "'";
            ResultSet rs = stm.executeQuery(sql);
            if (!rs.next())
                return;
            String[] heads = {"Call-number", "ISBN", "Title", "Author", "Amount", "Location"};
            System.out.println("-----------------------------------------------------");
            for (int i = 0; i < 6; ++i) { // books table 6 attributes
                try {
                    System.out.println(heads[i] + " : " + rs.getString(i + 1)); // attribute id starts with 1

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            //System.out.println("=====================================================");
        } catch (SQLException e1) {
            e1.printStackTrace();
            noException = false;
        }
    }


    /**
     * Print out the information of a borrow request
     * This function will select all data from Book table with the specific Call number and SID
     *
     * @param SID, Call_NO
     */
    private void printBorrowInfo(String SID, String Call) {
        try {
            Statement stm = conn.createStatement();
            String sql = "select SID, Call_NO, BORROW_DATE, DUE_DATE from borrow where sid = " + "'" + SID + "'and call_no = '" + Call + "'";
            ResultSet rs = stm.executeQuery(sql);
            if (!rs.next())
                return;
            String[] heads = {"Borrower", "Book", "Borrow Date", "Due Date"};
            System.out.println("-----------------------------------------------------");
            for (int i = 0; i < 4; ++i) {
                try {
                    System.out.println(heads[i] + " : " + rs.getString(i + 1));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            //System.out.println("=====================================================");
            return;
        } catch (SQLException e1) {
            e1.printStackTrace();
            noException = false;
            return;
        }
    }

    /**
     * Book Search
     * User need to input the ISBN of the book to search for the information
     * The function will first search the quantity of the book
     * If there are more than one is available, the function will print out the information of the book
     * Else will print "The book is not available at present"
     */
    private void searchBook() {
        System.out.println("Please input the ISBN of the book:");
        //Students who need to Search books, enter ISBN
        String line = in.nextLine();
        //Scan input data
        while (!(line.equalsIgnoreCase("exit")))
        //option "exit"
        {
            try {
                /**
                 * Create the statement and sql
                 */
                Statement stm = conn.createStatement();
                String sql = "select * from books where isbn = " + "'" + line + "'";
                //SQL: Select user input ISBN is from BOOKS
                ResultSet rs = stm.executeQuery(sql);

                while (rs.next())
                //Have record
                {
                    if (rs.getString("AMOUNT").equals("0")) {
                        System.out.println();
                        System.out.println("The book is not available at present");
                        //Check if from BOOKS where amount = 0, Then print The book is not available
                    } else {
                        System.out.println();
                        System.out.println("The book is available");
                        printBookInfo(rs.getString("ISBN"));
                    }
                }
                rs.close();
                stm.close();
                break;
            } catch (SQLException e) {
                noException = false;
                System.out.println("Please enter correct ISBN or enter exit to leave the search function");
                break;
            }

        }
    }


    /**
     * Borrow book
     * User will be require to input the sid of the student and the call_no of the book for borrowing a book
     * if the quantity of the book is lower than 1, it will print "the book is not available"
     * if the students had borrowed 5 books, it will print "you have already borrow 5 books"
     * if the students and an overdue book, it will print "You have at least one overdue book, you cannot make a new borrowing."
     * if successfully borrowed, it will call printBorrowInfo() to print out the borrowing information
     *
     * @return
     */
    private void borrow() {
        System.out.println("Please input the student no., call no. of the book to be borrowed: ");
        String line = in.nextLine();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate now = LocalDate.now();
        String todaydate = dtf.format(now);


        while (!(line.equalsIgnoreCase("exit"))) {
            //option "exit"
            String[] values = line.split(",");
            if (values.length != 2) {
                //check user input right format of input data (student no., call no.),
                // if wrong just input repeatedly
                System.out.println("The value number is expected to be 2\nPlease enter correct information:");
                line = in.nextLine();
            } else {
                //check user input right format of input data (student no., call no.);Then print them.
                for (int i = 0; i < values.length; ++i) {
                    values[i] = values[i].trim();
                }
                try {
                    Statement stm = conn.createStatement();

                    String sql = "Select * from borrow where sid ='" + values[0] + "'";
                    ResultSet rs = stm.executeQuery(sql);
                    while (rs.next()) {
                        if (compareDates(rs.getString("DUE_DATE"), todaydate)) {
                            throw new Exception("You have at least one overdue book, you cannot make a new borrowing.");
                        }
                    }


                    sql = "Select * from books where call_no ='" + values[1] + "'";
                    rs = stm.executeQuery(sql);
                    while (rs.next()) {
                        if (rs.getString("AMOUNT").equals("0")) {
                            throw new Exception("The book is not available at present");
                        }
                    }

                    sql = "Select * from students where sid ='" + values[0] + "'";
                    rs = stm.executeQuery(sql);
                    while (rs.next()) {
                        if (rs.getString("BORROW_NO").equals("5")) {
                            throw new Exception("You have already borrowed 5 books");
                        }
                    }

                    sql = "SELECT SID FROM RESERVE WHERE CALL_NO = '" + values[1] + "'";
                    rs = stm.executeQuery(sql);
                    if (rs.next() == true && !rs.getString(1).equals(values[0])) {
                        throw new Exception("The book is reserved by another student");
                    }


                    sql = "INSERT INTO BORROW VALUES('" + values[0] + "', " + //SID
                            "'" + values[1] + "', " + //call number
                            "current_date, " + //borrow date
                            "current_date+28, " +//due date
                            "0)";//renew times =0;
                    // sql = "INSERT INTO BORROW VALUES('" + values[0] + "', " + //SID
                    //         "'" + values[1] + "', " + //call number
                    //         "to_date('22/04/2022','dd/mm/yyyy'), " + //borrow date
                    //         "to_date('22/04/2022','dd/mm/yyyy')+28, " +//due date
                    //         "0)";//renew times =0;

                    stm.executeUpdate(sql);
                    stm.close();
                    System.out.println();
                    System.out.println("The borrowing succeeded");
                    printBorrowInfo(values[0], values[1]);
                    break;

                } catch (SQLException e) {
                    e.printStackTrace();
                    noException = false;
                    break;

                } catch (Exception e) {
                    System.out.println();
                    System.out.println(e.getMessage());
                    break;
                }
            }
        }
    }


    /**
     * Return Book
     **/
    public void returnBooks() {
        System.out.println("Please input the student no., call no. of the book to be returned: ");
        //Students who need to return books, enter student no., call no.
        String line = in.nextLine();
        //Scan input data
        String[] input = line.split(",");
        //Except for the information of " , ", all other input must be
        for (int i = 0; i < input.length; ++i)
            input[i] = input[i].trim(); //separate two input data

        while (!(line.equalsIgnoreCase("exit"))) {
            //option "exit"

            try {
                Statement stm = conn.createStatement();
                String sql = "SELECT * FROM BORROW WHERE SID = '" + input[0] + "' " +
                        "AND CALL_NO = '" + input[1] + "' ";
                //First find the input data, whether it is in BORROW
                ResultSet rs = stm.executeQuery(sql);
                /**
                 if (!rs.next()) {
                 System.out.println("Wrong student no. or call no.");
                 //If these data are not in BORROW, it means input error
                 break;

                 } else*/while(rs.next()) {
                    //Entered correctly
                    Statement stm1 = conn.createStatement();
                    String sql1 = "DELETE FROM BORROW WHERE SID = '" + input[0] + "' " +
                            "AND CALL_NO = '" + input[1] + "' ";
                    //SQL: According to the user's input, delete the records that have borrowed books
                    stm1.executeUpdate(sql1);
                    stm1.close();

                    Statement stm2 = conn.createStatement();
                    String sql2 = "UPDATE STUDENTS SET BORROW_NO = BORROW_NO - 1 WHERE SID = '" + input[0] + "'";
                    //SQL: According to the user's input data (SID), Update Student BORROW_NO
                    stm2.executeUpdate(sql2);
                    stm2.close();

                    System.out.println("The return succeeded");
                }
                rs.close();
                stm.close();
                break;

            } catch (SQLException e) {
                noException = false;
                System.out.println("The return failed");
                break;
            }

        }

    }


    /**
     * Book Renewal
     * Given student A and book B, student A can renew book B if the following conditions are satisfied:
     * 1. None of the books student A borrowed is overdue.
     * 2. Student A hasn’t renewed book B after he borrowed it.
     * 3. This renewal is allowed only during the 2nd half of B’s borrow period.
     * 4. Book B is not reserved by any students.
     * The corresponding borrow record should be updated upon success.
     **/
    private void renewBook() {
        //find today
        //get the actual date of today by imported library: LocalDate
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate now = LocalDate.now();
        String todaydate = dtf.format(now);
        System.out.println("Today: " + todaydate);

        String[] todaystring = todaydate.split("-");
        for (int i = 0; i < todaystring.length; ++i) { //[0]: dd; [1]: mm; [2]: yyyy
            todaystring[i] = todaystring[i].trim();
        }
        System.out.println();
        int today = Integer.parseInt(todaystring[2] + todaystring[1] + todaystring[0]);
        //convert string date to integer date for comparison
        //String todaystring = "22-04-2022"; 	//for testing
        //int today = 20220422; 	//for testing
        //end find today

        System.out.println("Please input your student no., call no. of the book to be renewed: ");
        String line = in.nextLine();
        String[] input = line.split(",");
        for (int i = 0; i < input.length; ++i)
            input[i] = input[i].trim();         //separate SID and CALL_NO into two inputs

        while (!(line.equalsIgnoreCase("exit"))) { //4 validations
            try {
                //1: check whether the book can be renewed by comparing Renew_Time < 0
                //2: check whether the renewal is available by finding whether the difference between today and due date < 14
                System.out.println();
                int[] todayint = new int[todaystring.length]; //separate the dd-mm-yyyy for calculating today (2)
                for (int i = 0; i < todayint.length; i++) {
                    todayint[i] = Integer.parseInt(todaystring[i]); //0: dd, 1: mm, 2: yyyy
                }
                //int[] todayint = {22, 4, 2022}; 	//for testing

                Statement stm2 = conn.createStatement();
                String sql2 = "SELECT to_char(DUE_DATE, 'dd-MM-yyyy') AS DUE_DATE, Renew_Time FROM BORROW WHERE SID = " + "'" + input[0] + "' AND CALL_NO = '" + input[1] + "'";
                //SQL: retrieve due date and renew_time of the specific student and book for validations (1, 2)

                ResultSet rs2 = stm2.executeQuery(sql2);
                while (rs2.next()) {
                    //check renew time
                    int renewtime = rs2.getInt("Renew_Time");
                    if (renewtime != 0) {
                        System.out.println("This book cannot be renewed again.");
                        return;
                    }

                    //check the availability of renewal
                    String duedatestring = rs2.getString("DUE_DATE"); //dd-MM-yyyy
                    String[] values = duedatestring.split("-");
                    for (int i = 0; i < values.length; ++i) { //values.length
                        values[i] = values[i].trim();
                    }
                    int duedate = Integer.parseInt(values[2] + values[1] + values[0]);

                    int dayAdd = 14; //half of the borrow period

                    if ((todayint[1] == 4 || todayint[1] == 6 || todayint[1] == 9 || todayint[1] == 11) && todayint[0] + dayAdd > 30) {
                        //counting the days for months having 30 days once the date is in next month
                        todayint[1]++;
                        todayint[0] = dayAdd - (30 - todayint[0]);

                    } else if ((todayint[1] == 1 || todayint[1] == 3 || todayint[1] == 5 || todayint[1] == 7 ||
                            todayint[1] == 8 || todayint[1] == 10 || todayint[1] == 12) && todayint[0] + dayAdd > 31) {
                        //counting the days for months having 31 days once the date is in next month
                        todayint[1]++;
                        todayint[0] = dayAdd - (31 - todayint[0]);

                    } else if ((todayint[1] == 2) && todayint[0] + dayAdd > 28) {
                        //counting the days for February having 28 days once the date is in next month
                        todayint[1]++;
                        todayint[0] = dayAdd - (28 - todayint[0]);
                    } else todayint[0] += dayAdd; //counting the days in the same month

                    int newtodayy; //for joining the calculated date after 14 days
                    if (todayint[1] < 10 && todayint[0] < 10)
                        newtodayy = Integer.parseInt(todayint[2] + "0" + todayint[1] + "0" + todayint[0]);
                    else if (todayint[1] < 10)
                        newtodayy = Integer.parseInt(todayint[2] + "0" + todayint[1] + todayint[0]);
                    else if (todayint[0] < 10)
                        newtodayy = Integer.parseInt(todayint[2] + todayint[1] + "0" + todayint[0]);
                    else newtodayy = Integer.parseInt(todayint[2] + "" + todayint[1] + "" + todayint[0]);

                    if (newtodayy < duedate) { //if today is not in the second half of borrow period
                        System.out.println("The renewal is not yet available."); //invalid -> return immediately
                        return;
                    }
                }
                rs2.close();
                stm2.close();


                //3: check for all overdue books
                Statement stm = conn.createStatement();
                String sql = "SELECT to_char(DUE_DATE, 'dd-MM-yyyy') AS DUE_DATE FROM BORROW WHERE SID = " + "'" + input[0] + "'";
                //SQL: retrieve due date of the specific student and all books he/she borrowed for validation (3)

                ResultSet rs = stm.executeQuery(sql);

                while (rs.next()) {
                    String duedatestring = rs.getString("DUE_DATE"); //dd-MM-yyyy
                    String[] values = duedatestring.split("-");

                    for (int i = 0; i < values.length; ++i) { //separate dd-mm-yyyy
                        values[i] = values[i].trim();
                    }

                    int duedate = Integer.parseInt(values[2] + values[1] + values[0]);
                    //convert string date to integer date for comparison

                    if (duedate < today) { //due date is in the past
                        System.out.println("You have at least one overdue book, you cannot make a new borrowing.");
                        return;
                    }
                }
                rs.close();
                stm.close();

                //4: check whether the book is reserved by another student
                Statement stm3 = conn.createStatement();
                String sql3 = "SELECT Count(CALL_NO) FROM RESERVE WHERE RESERVE.CALL_NO = '" + input[1] + "' GROUP BY CALL_NO";
                //SQL: retrieve the number of reserve of the specific book for validation (3)
                ResultSet rs3 = stm3.executeQuery(sql3);
                while (rs3.next()) {
                    int count = rs3.getInt("Count(CALL_NO)"); //double-checking
                    if (count > 0) { //count > 0: the book is in reserve table
                        System.out.println("The book is reserved by another student.");
                        return;
                    }
                }
                //output null: the book is not in reserve table
                rs3.close();
                stm3.close();

                //pass all 4 validations --> renew process
                Statement stm4 = conn.createStatement();
                String sql4 = "UPDATE BORROW SET DUE_DATE = (TO_DATE('" + todaydate + "','DD-MM-YYYY')) + 28, " +
                        "RENEW_TIME = RENEW_TIME +1 WHERE SID = '" + input[0] + "' AND CALL_NO = '" + input[1] + "'";
                //SQL: update the due date extended by 28 days since today
                stm4.executeUpdate(sql4);
                stm4.close();
                System.out.println("The borrowed book is renewed successfully.");
                printBorrowInfo(input[0], input[1]);
                break;
            } catch (SQLException b) {
                b.printStackTrace();
                noException = false;
                System.out.println("Please enter correct information.");
                line = in.nextLine();
            }
        }
    }


    /**
     * Book Reservation
     * Given student A and book B, student A can reserve book B if the following conditions are satisfied:
     * 1. The available amount of B is 0.
     * 2. Book B is not borrowed by Student A.
     * 3. Student A doesn’t hold any other reservation request.
     * The corresponding reserve record would be updated upon success.
     */
    private void reserveBook() {
        System.out.println("Please input your student no., call no. of the book to be reserved:");

        String line = in.nextLine();

        if (line.equalsIgnoreCase("exit"))
            return;

        String[] values = line.split(",");
        for (int i = 0; i < values.length; ++i)
            values[i] = values[i].trim();

        try {

            Statement stm = conn.createStatement();

            //check 3rd condition
            String sql = "SELECT * FROM RESERVE " +
                    "WHERE SID = '" + values[0] + "'";

            ResultSet rs = stm.executeQuery(sql);

            if (rs.next()) {
                throw new Exception("Multiple reservations are not allowed");
            }


            //check 2nd condition
            sql = "SELECT * FROM BORROW " +
                    "WHERE SID = '" + values[0] + "' AND CALL_NO = '" + values[1] + "'";

            rs = stm.executeQuery(sql);

            if (rs.next()) {
                throw new Exception("Reserving a borrowed book is not allowed");
            }


            //check 1st condition
            sql = "SELECT AMOUNT FROM BOOKS " +
                    "WHERE CALL_NO = '" + values[1] + "'";

            rs = stm.executeQuery(sql);

            while (rs.next()) {
                if (!rs.getString(1).equals("0")) {
                    throw new Exception("The book is now available. No reservation is required");
                }
            }


            //reserve
            sql = "INSERT INTO RESERVE VALUES(" + "'" + values[0] + "', " + // SID
                    "'" + values[1] + "', " + // CALL_NO
                    "to_date('" + java.time.LocalDate.now() + "', 'yyyy-mm-dd')" + // DATE
                    ")";

            stm.executeUpdate(sql);

            //check reserve table
            sql = "SELECT * FROM RESERVE " +
                    "WHERE SID = '" + values[0] + "'";

            rs = stm.executeQuery(sql);

            if (!rs.next())
                throw new Exception("The reservation failed");


            //print the record
            System.out.println();
            System.out.println("The reservation succeeded");
            System.out.println("-----------------------------------------------------");
            String[] heads = {"Student_no", "Reserved_Book", "Requested_Date"};
            for (int i = 0; i < 3; ++i) { // reserve table 3 attributes
                try {
                    System.out.println(heads[i] + " : " + rs.getString(i + 1)); // attribute id starts with 1
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            rs.close();
            stm.close();

        } catch (SQLException e) {
            e.printStackTrace();
            noException = false;

            //print error message
        } catch (Exception e) {
            System.out.println();
            System.out.println(e.getMessage());
        }

    }


    /**
     * Close the manager. Do not change this function.
     */
    public void close() {
        System.out.println("Thanks for using this manager! Bye...");
        try {
            if (conn != null)
                conn.close();
            if (proxySession != null) {
                proxySession.disconnect();
            }
            in.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor of Library manager
     */
    public LibraryManager() {
        System.out.println("Welcome to use this manager!");
        in = new Scanner(System.in);
    }

    /**
     * this function is for comparing two date
     * if first date is earlier than second date, it will return true,
     * else will return false
     *
     * @param d1
     * @param d2
     * @return Boolean
     */
    public Boolean compareDates(String d1, String d2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date1 = sdf.parse(d1);
            Date date2 = sdf.parse(d2);

            if (date1.before(date2)) {
                return true;
            } else {
                return false;
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Main function
     *
     * @param args
     */
    public static void main(String[] args) {
        LibraryManager manager = new LibraryManager();
        if (!manager.loginProxy()) {
            System.out.println("Login proxy failed, please re-examine your username and password!");
            return;
        }
        if (!manager.loginDB()) {
            System.out.println("Login database failed, please re-examine your username and password!");
            return;
        }
        System.out.println("Login succeed!");
        try {
            manager.run();
        } finally {
            manager.close();
        }
    }
}
