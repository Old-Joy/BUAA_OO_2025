// [HW15 最终修改] 请确保所有 import 来自于第十五次作业的官方包 (例如 library3)
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryCommand;
import com.oocourse.library3.LibraryCloseCmd;
import com.oocourse.library3.LibraryOpenCmd;
import com.oocourse.library3.LibraryQcsCmd;
import com.oocourse.library3.LibraryReqCmd;
import static com.oocourse.library3.LibraryIO.SCANNER;

import java.time.LocalDate;
import java.util.Map;

public class MainClass {
    public static void main(String[] args) {
        LibraryController libraryController = new LibraryController();
        Map<LibraryBookIsbn, Integer> inventory = SCANNER.getInventory();
        libraryController.initializeInventory(inventory);
        LocalDate lastCommandDate = null;
        while (true) {
            LibraryCommand command = SCANNER.nextCommand();
            if (command == null) { break; }
            LocalDate currentDate = command.getDate();
            if (lastCommandDate != null && !currentDate.isEqual(lastCommandDate)) {
                for (LocalDate date = lastCommandDate.plusDays(1);
                    date.isBefore(currentDate); date = date.plusDays(1)) {
                    // 对每一个空闲日，执行其闭馆时应结算的罚款
                    libraryController.processDailyPenalties(date);
                }
            }
            libraryController.setCurrentDate(currentDate);
            if (command instanceof LibraryOpenCmd) {
                libraryController.handleOpen((LibraryOpenCmd) command);
            } else if (command instanceof LibraryCloseCmd) {
                libraryController.handleClose((LibraryCloseCmd) command);
            } else if (command instanceof LibraryQcsCmd) {
                libraryController.handleQueryCreditScore((LibraryQcsCmd) command);
            } else if (command instanceof LibraryReqCmd) {
                LibraryReqCmd req = (LibraryReqCmd) command;
                switch (req.getType()) {
                    case QUERIED:
                        libraryController.handleQuery(req);
                        break;
                    case BORROWED:
                        libraryController.handleBorrow(req);
                        break;
                    case ORDERED:
                        libraryController.orderNewBook(req);
                        break;
                    case RETURNED:
                        libraryController.handleReturn(req);
                        break;
                    case PICKED:
                        libraryController.getOrderedBook(req);
                        break;
                    case READ:
                        libraryController.handleRead(req);
                        break;
                    case RESTORED:
                        libraryController.handleRestore(req);
                        break;
                    default:
                        break;
                }
            }
            lastCommandDate = currentDate;
        }
    }
}