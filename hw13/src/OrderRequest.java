// OrderRequest.java
import com.oocourse.library1.LibraryBookIsbn;

import java.time.LocalDate;
import java.util.Objects;

public class OrderRequest {
    private final String studentId;
    private final LibraryBookIsbn bookIsbn;
    private final LocalDate orderDate; // Date the request was made
    private BookCopy assignedBookCopy; // Null until a book is moved to AO
    private LocalDate bookReadyAtAoDate; // Date book was moved to AO
    private LocalDate expiryDateAtAo; // Last day it's reserved at AO

    public OrderRequest(String studentId, LibraryBookIsbn bookIsbn, LocalDate orderDate) {
        this.studentId = studentId;
        this.bookIsbn = bookIsbn;
        this.orderDate = orderDate;
        this.assignedBookCopy = null;
        this.bookReadyAtAoDate = null;
        this.expiryDateAtAo = null;
    }

    public String getStudentId() {
        return studentId;
    }

    public LibraryBookIsbn getBookIsbn() {
        return bookIsbn;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public BookCopy getAssignedBookCopy() {
        return assignedBookCopy;
    }

    public void setAssignedBookCopy(BookCopy assignedBookCopy) {
        this.assignedBookCopy = assignedBookCopy;
    }

    public LocalDate getBookReadyAtAoDate() {
        return bookReadyAtAoDate;
    }

    public void setBookReadyAtAoDate(LocalDate bookReadyAtAoDate) {
        this.bookReadyAtAoDate = bookReadyAtAoDate;
    }

    public LocalDate getExpiryDateAtAo() {
        return expiryDateAtAo;
    }

    public void setExpiryDateAtAo(LocalDate expiryDateAtAo) {
        this.expiryDateAtAo = expiryDateAtAo;
    }

    public boolean isAssigned() {
        return this.assignedBookCopy != null;
    }

    public boolean isExpiredBy(LocalDate currentDate, boolean isOpeningSort) {
        if (!isAssigned() || expiryDateAtAo == null) {
            return false;
        }
        if (isOpeningSort) {
            return expiryDateAtAo.isBefore(currentDate);
        } else {
            return expiryDateAtAo.isEqual(currentDate);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        OrderRequest that = (OrderRequest) o;
        return Objects.equals(studentId, that.studentId) &&
                Objects.equals(bookIsbn, that.bookIsbn) &&
                Objects.equals(orderDate, that.orderDate) &&
                Objects.equals(assignedBookCopy, that.assignedBookCopy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, bookIsbn, orderDate, assignedBookCopy);
    }
}