// BookCopy.java
import com.oocourse.library1.LibraryBookId;
import com.oocourse.library1.LibraryBookIsbn;
import com.oocourse.library1.LibraryBookState;
import com.oocourse.library1.LibraryTrace;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BookCopy {
    private final LibraryBookId id;
    private final LibraryBookIsbn isbn;
    private LibraryBookState currentLocation;
    private final List<LibraryTrace> movementHistory;
    private LibUser currentHolder; // If location is USER
    private OrderRequest reservationInfo; // If location is APPOINTMENT_OFFICE

    public BookCopy(LibraryBookId id) {
        this.id = id;
        this.isbn = new LibraryBookIsbn(id.getType(), id.getUid()); // Derive ISBN from BookId
        this.currentLocation = LibraryBookState.BOOKSHELF; // All books start on the bookshelf
        this.movementHistory = new ArrayList<>();
        this.currentHolder = null;
        this.reservationInfo = null;
    }

    public LibraryBookId getId() {
        return id;
    }

    public LibraryBookIsbn getIsbn() {
        return isbn;
    }

    public LibraryBookState getCurrentLocation() {
        return currentLocation;
    }

    public List<LibraryTrace> getMovementHistory() {
        return new ArrayList<>(movementHistory); // Return a copy
    }

    public void moveTo(LibraryBookState newLocation, LocalDate date) {
        if (this.currentLocation != newLocation) { // Record history only if location changes
            this.movementHistory.add(new LibraryTrace(date, this.currentLocation, newLocation));
        }
        this.currentLocation = newLocation;

        // Clear holder/reservation if no longer relevant
        if (newLocation != LibraryBookState.USER) {
            this.currentHolder = null;
        }
        if (newLocation != LibraryBookState.APPOINTMENT_OFFICE) {
            this.reservationInfo = null;
        }
    }

    public LibUser getCurrentHolder() {
        return currentHolder;
    }

    public void setCurrentHolder(LibUser currentHolder) {
        this.currentHolder = currentHolder;
    }

    public OrderRequest getReservationInfo() {
        return reservationInfo;
    }

    public void setReservationInfo(OrderRequest reservationInfo) {
        this.reservationInfo = reservationInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BookCopy bookCopy = (BookCopy) o;
        return Objects.equals(id, bookCopy.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}