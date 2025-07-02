import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.annotation.SendMessage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LibUser {
    private final String studentId;
    private final Set<BookCopy> heldBooks;
    private OrderRequest activeOrderRequest;
    private BookCopy currentReadingBook; // 当前正在阅读的书籍
    private LocalDate lastReadDate;      // 上次开始阅读的日期
    private int creditScore;
    private static final int INITIAL_CREDIT = 100;
    private static final int MAX_CREDIT = 180;
    private static final int MIN_CREDIT = 0;

    public LibUser(String studentId) {
        this.studentId = studentId;
        this.heldBooks = new HashSet<>();
        this.activeOrderRequest = null;
        this.currentReadingBook = null;
        this.lastReadDate = null;
        this.creditScore = INITIAL_CREDIT;
    }

    public String getStudentId() {
        return studentId;
    }

    public Set<BookCopy> getHeldBooks() {
        return new HashSet<>(heldBooks); // Return a copy
    }

    @SendMessage(from = "LibraryController", to = "LibUser")
    public void addBook(BookCopy book) {
        this.heldBooks.add(book);
    }

    public void removeBook(BookCopy book) {
        this.heldBooks.remove(book);
    }

    @SendMessage(from = "LibraryController", to = "LibUser")
    public OrderRequest getActiveOrderRequest() {
        return activeOrderRequest;
    }

    @SendMessage(from = "LibraryController", to = "LibUser")
    public void setActiveOrderRequest(OrderRequest activeOrderRequest) {
        this.activeOrderRequest = activeOrderRequest;
    }

    public BookCopy getCurrentReadingBook() {
        return currentReadingBook;
    }

    public void setCurrentReadingBook(BookCopy book, LocalDate date) {
        this.currentReadingBook = book;
        this.lastReadDate = date;
    }

    // 检查用户在指定日期是否已经有未归还的阅读书籍
    public boolean hasUnreturnedReadBookOnDate(LocalDate currentDate) {
        return this.currentReadingBook != null && this.lastReadDate != null
            && this.lastReadDate.isEqual(currentDate);
    }

    public boolean isHoldingBookType(LibraryBookIsbn.Type type) {
        for (BookCopy book : heldBooks) {
            if (book.getIsbn().getType() == type) {
                return true;
            }
        }
        return false;
    }

    public boolean isHoldingBookIsbn(LibraryBookIsbn isbn) {
        for (BookCopy book : heldBooks) {
            if (book.getIsbn().equals(isbn)) {
                return true;
            }
        }
        return false;
    }

    // Checks if user can borrow a book of this ISBN based on what they currently hold
    public boolean canSatisfyBorrowingLimitFor(LibraryBookIsbn bookIsbnToBorrow) {
        if (bookIsbnToBorrow.isTypeB()) {
            if (isHoldingBookType(LibraryBookIsbn.Type.B)) {
                return false; // Already holding a B type book
            }
        } else if (bookIsbnToBorrow.isTypeC()) {
            if (isHoldingBookIsbn(bookIsbnToBorrow)) {
                return false; // Already holding this C type ISBN
            }
        }
        return true;
    }

    // Checks if user can order a book of this ISBN based on what they currently hold
    public boolean canSatisfyOrderingLimitFor(LibraryBookIsbn bookIsbnToOrder) {
        // Type A cannot be ordered
        if (bookIsbnToOrder.isTypeA()) {
            return false;
        }
        // If already holding a B book, cannot order any B book
        if (bookIsbnToOrder.isTypeB() && isHoldingBookType(LibraryBookIsbn.Type.B)) {
            return false;
        }
        // If already holding a C book of specific ISBN, cannot order that same ISBN
        if (bookIsbnToOrder.isTypeC() && isHoldingBookIsbn(bookIsbnToOrder)) {
            return false;
        }
        return true;
    }

    public int getCreditScore() {
        return this.creditScore;
    }

    public void addCredit(int amount) {
        this.creditScore = Math.min(this.creditScore + amount, MAX_CREDIT);
    }

    public void deductCredit(int amount) {
        this.creditScore = Math.max(this.creditScore - amount, MIN_CREDIT);
    }

    // 检查是否允许阅读某类书籍
    public boolean canRead(LibraryBookIsbn.Type type) {
        if (type == LibraryBookIsbn.Type.A) {
            return this.creditScore >= 40; // 读A类书需信用分不低于40
        }
        return this.creditScore > 0; // 读B/C类书信用分需大于0
    }

    // 检查是否允许借阅
    @SendMessage(from = "LibraryController", to = "LibUser")
    public boolean canBorrow() {
        return this.creditScore >= 60; // 借书需信用分不低于60
    }

    // 检查是否允许预约
    @SendMessage(from = "LibraryController", to = "LibUser")
    @SendMessage(from = "LibUser", to = "LibraryController")
    public boolean canOrder() {
        return this.creditScore >= 100; // 预约需信用分不低于100
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LibUser libUser = (LibUser) o;
        return Objects.equals(studentId, libUser.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId);
    }
}