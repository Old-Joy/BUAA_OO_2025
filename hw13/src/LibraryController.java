import com.oocourse.library1.LibraryBookId;
import com.oocourse.library1.LibraryBookIsbn;
import com.oocourse.library1.LibraryBookState;
import com.oocourse.library1.LibraryCloseCmd;
import com.oocourse.library1.LibraryMoveInfo;
import com.oocourse.library1.LibraryOpenCmd;
import com.oocourse.library1.LibraryReqCmd;

import static com.oocourse.library1.LibraryIO.PRINTER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LibraryController {
    private LocalDate currentDate;
    private final Map<LibraryBookId, BookCopy> allBookCopies;
    private final Map<LibraryBookIsbn, List<BookCopy>> booksByIsbn;
    private final Map<String, LibUser> allUsers;
    private final List<OrderRequest> pendingOrderRequests;
    private final Map<BookCopy, OrderRequest> booksAtAppointmentOffice;

    public LibraryController() {
        this.allBookCopies = new HashMap<>();
        this.booksByIsbn = new HashMap<>();
        this.allUsers = new HashMap<>();
        this.pendingOrderRequests = new ArrayList<>();
        this.booksAtAppointmentOffice = new HashMap<>();
    }

    public void setCurrentDate(LocalDate date) {
        this.currentDate = date;
    }

    public void initializeInventory(Map<LibraryBookIsbn, Integer> inventory) {
        for (Map.Entry<LibraryBookIsbn, Integer> entry : inventory.entrySet()) {
            LibraryBookIsbn isbn = entry.getKey();
            int count = entry.getValue();
            booksByIsbn.putIfAbsent(isbn, new ArrayList<>());
            for (int i = 1; i <= count; i++) {
                //副本号从1开始依递增顺序编号, e.g., "01", "02"
                String copyIdStr = String.format("%02d", i);
                LibraryBookId bookId = new LibraryBookId(isbn.getType(), isbn.getUid(), copyIdStr);
                BookCopy bookCopy = new BookCopy(bookId);
                allBookCopies.put(bookId, bookCopy);
                booksByIsbn.get(isbn).add(bookCopy);
            }
        }
    }

    private List<BookCopy> getAvailableCopiesOnShelf(LibraryBookIsbn isbn) {
        return booksByIsbn.getOrDefault(isbn, new ArrayList<>())
                .stream()
                .filter(b -> b.getCurrentLocation() == LibraryBookState.BOOKSHELF)
                .sorted(Comparator.comparing(b -> b.getId().getCopyId()))
                .collect(Collectors.toList());
    }

    private LibUser getOrCreateUser(String studentId) {
        return allUsers.computeIfAbsent(studentId, LibUser::new);
    }

    private void organizeBooks(boolean isOpeningSort) {
        List<LibraryMoveInfo> moves = new ArrayList<>();
        List<BookCopy> booksToRemoveFromAo = new ArrayList<>();
        for (Map.Entry<BookCopy, OrderRequest> entry : booksAtAppointmentOffice.entrySet()) {
            BookCopy book = entry.getKey();
            OrderRequest order = entry.getValue();
            if (order.isExpiredBy(currentDate, isOpeningSort)) {
                moves.add(new LibraryMoveInfo(book.getId(),
                    LibraryBookState.APPOINTMENT_OFFICE, LibraryBookState.BOOKSHELF));
                book.moveTo(LibraryBookState.BOOKSHELF, currentDate);
                booksToRemoveFromAo.add(book);
                LibUser user = getOrCreateUser(order.getStudentId());
                if (user.getActiveOrderRequest() == order) {
                    user.setActiveOrderRequest(null); // Reservation failed
                }
            }
        }
        booksToRemoveFromAo.forEach(booksAtAppointmentOffice::remove);
        for (BookCopy book : allBookCopies.values()) {
            if (book.getCurrentLocation() == LibraryBookState.BORROW_RETURN_OFFICE) {
                moves.add(new LibraryMoveInfo(book.getId(),
                    LibraryBookState.BORROW_RETURN_OFFICE, LibraryBookState.BOOKSHELF));
                book.moveTo(LibraryBookState.BOOKSHELF, currentDate);
            }
        }
        List<OrderRequest> fulfilledOrders = new ArrayList<>();
        pendingOrderRequests.sort(Comparator.comparing(OrderRequest::getOrderDate));
        for (OrderRequest order : pendingOrderRequests) {
            List<BookCopy> availableCopies = getAvailableCopiesOnShelf(order.getBookIsbn());
            if (!availableCopies.isEmpty()) {
                BookCopy bookToReserve = availableCopies.get(0);
                moves.add(new LibraryMoveInfo(bookToReserve.getId(), LibraryBookState.BOOKSHELF,
                    LibraryBookState.APPOINTMENT_OFFICE, order.getStudentId()));
                bookToReserve.moveTo(LibraryBookState.APPOINTMENT_OFFICE, currentDate);
                bookToReserve.setReservationInfo(order);
                order.setAssignedBookCopy(bookToReserve);
                order.setBookReadyAtAoDate(currentDate);
                LocalDate expiry;
                if (isOpeningSort) {
                    expiry = currentDate.plusDays(4);
                } else {
                    expiry = currentDate.plusDays(5);
                }
                order.setExpiryDateAtAo(expiry);
                booksAtAppointmentOffice.put(bookToReserve, order);
                fulfilledOrders.add(order);
            }
        }
        pendingOrderRequests.removeAll(fulfilledOrders);
        PRINTER.move(currentDate, moves);
    }

    public void handleOpen(LibraryOpenCmd cmd) {
        organizeBooks(true);
    }

    public void handleClose(LibraryCloseCmd cmd) {
        organizeBooks(false);
    }

    public void handleQuery(LibraryReqCmd cmd) {
        LibraryBookId bookIdToQuery = cmd.getBookId(); // Query is by specific copy
        BookCopy book = allBookCopies.get(bookIdToQuery);
        if (book != null) {
            PRINTER.info(currentDate, bookIdToQuery, book.getMovementHistory());
        } else {
            // Should not happen based on "输入指令中操作的书籍编号，一定在图书馆中存在"
            // But as a fallback, print empty history or handle as error
            PRINTER.info(currentDate, bookIdToQuery, new ArrayList<>());
        }
    }

    public void handleBorrow(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        LibraryBookIsbn isbnToBorrow = cmd.getBookIsbn(); // Borrow is by ISBN
        LibUser user = getOrCreateUser(studentId);

        // 1. A类书不可借阅
        if (isbnToBorrow.isTypeA()) {
            PRINTER.reject(cmd);
            return;
        }

        // 2. Check user's borrowing limits for B and C books
        if (!user.canSatisfyBorrowingLimitFor(isbnToBorrow)) {
            PRINTER.reject(cmd);
            return;
        }

        // 3. Check if book has copies on shelf
        List<BookCopy> availableCopies = getAvailableCopiesOnShelf(isbnToBorrow);
        if (availableCopies.isEmpty()) {
            PRINTER.reject(cmd); // "若欲借阅的书籍无余本在架，借阅失败"
            return;
        }

        // Success
        BookCopy borrowedBook = availableCopies.get(0); // Pick one, e.g., first by copyId
        borrowedBook.moveTo(LibraryBookState.USER, currentDate);
        borrowedBook.setCurrentHolder(user);
        user.addBook(borrowedBook);

        PRINTER.accept(cmd, borrowedBook.getId());
    }

    public void handleReturn(LibraryReqCmd cmd) {
        LibraryBookId bookIdToReturn = cmd.getBookId();
        LibUser user = getOrCreateUser(cmd.getStudentId());
        BookCopy book = allBookCopies.get(bookIdToReturn);
        if (book != null && book.getCurrentLocation() == LibraryBookState.USER
            && book.getCurrentHolder() == user) {
            user.removeBook(book);
            book.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate);
            PRINTER.accept(cmd); // "还书立即成功"
        } else {
            if (book != null) {
                if (book.getCurrentHolder() != null) { book.getCurrentHolder().removeBook(book); }
                if (user != book.getCurrentHolder()) { user.removeBook(book); }
                book.moveTo(LibraryBookState.BORROW_RETURN_OFFICE, currentDate);
            }
            PRINTER.accept(cmd);
        }
    }

    public void handleOrder(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        LibraryBookIsbn isbnToOrder = cmd.getBookIsbn();
        LibUser user = getOrCreateUser(studentId);
        if (isbnToOrder.isTypeA()) {
            PRINTER.reject(cmd);
            return;
        }
        if (user.getActiveOrderRequest() != null) {
            PRINTER.reject(cmd);
            return;
        }
        if (!user.canSatisfyOrderingLimitFor(isbnToOrder)) {
            PRINTER.reject(cmd);
            return;
        }
        OrderRequest newOrder = new OrderRequest(studentId, isbnToOrder, currentDate);
        user.setActiveOrderRequest(newOrder);
        pendingOrderRequests.add(newOrder);
        PRINTER.accept(cmd);
    }

    public void handlePick(LibraryReqCmd cmd) {
        String studentId = cmd.getStudentId();
        LibraryBookIsbn isbnToPick = cmd.getBookIsbn();
        LibUser user = getOrCreateUser(studentId);
        OrderRequest activeOrder = user.getActiveOrderRequest();
        if (activeOrder == null || !activeOrder.isAssigned()
            || !activeOrder.getBookIsbn().equals(isbnToPick)) {
            PRINTER.reject(cmd);
            return;
        }
        BookCopy reservedBook = activeOrder.getAssignedBookCopy();
        if (reservedBook == null ||
            reservedBook.getCurrentLocation() != LibraryBookState.APPOINTMENT_OFFICE ||
            !booksAtAppointmentOffice.getOrDefault(reservedBook, null).equals(activeOrder)) {
            PRINTER.reject(cmd);
            return;
        }
        if (activeOrder.isExpiredBy(currentDate, true)) {
            PRINTER.reject(cmd); // Reservation expired
            return;
        }

        // 4. Check if picking up violates borrowing limits
        if (!user.canSatisfyBorrowingLimitFor(reservedBook.getIsbn())) {
            PRINTER.reject(cmd);
            return;
        }

        // Success
        booksAtAppointmentOffice.remove(reservedBook); // Remove from AO tracking
        reservedBook.moveTo(LibraryBookState.USER, currentDate);
        reservedBook.setCurrentHolder(user);
        // reservedBook.setReservationInfo(null); // moveTo already clears it

        user.addBook(reservedBook);
        user.setActiveOrderRequest(null); // Order fulfilled

        PRINTER.accept(cmd, reservedBook.getId());
    }
}