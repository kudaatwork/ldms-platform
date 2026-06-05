package projectlx.user.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.SupportTicket;
import projectlx.user.management.model.SupportTicketMessage;
import projectlx.user.management.model.SupportTicketMessageAuthorRole;
import projectlx.user.management.model.SupportTicketMessageVisibility;
import projectlx.user.management.model.SupportTicketStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.repository.SupportTicketMessageRepository;
import projectlx.user.management.repository.SupportTicketRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.dtos.SupportTicketDto;
import projectlx.user.management.utils.dtos.SupportTicketMessageDto;
import projectlx.user.management.utils.requests.SupportTicketExportFilterRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SupportTicketOperationsSupport {

    private static final DateTimeFormatter EXPORT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] EXPORT_HEADERS = {
            "Ticket", "Subject", "Status", "Priority", "Category", "Requester", "Handler",
            "Opened", "Resolved", "Closed", "Messages"
    };

    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketMessageRepository supportTicketMessageRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public Optional<SupportTicket> findActiveTicket(Long id) {
        return supportTicketRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
    }

    public Optional<SupportTicket> findActiveTicketForRequester(Long id, String username) {
        return supportTicketRepository.findByIdAndRequesterUsernameAndEntityStatusNot(id, username, EntityStatus.DELETED);
    }

    public SupportTicketDto toDto(SupportTicket ticket, boolean includeInternalMessages) {
        SupportTicketDto dto = modelMapper.map(ticket, SupportTicketDto.class);
        dto.setMessages(loadMessages(ticket.getId(), includeInternalMessages));
        return dto;
    }

    public List<SupportTicketDto> toDtoList(List<SupportTicket> tickets) {
        return modelMapper.map(tickets, new TypeToken<List<SupportTicketDto>>() {}.getType());
    }

    public List<SupportTicketMessageDto> loadMessages(Long ticketId, boolean includeInternalMessages) {
        List<SupportTicketMessage> messages = includeInternalMessages
                ? supportTicketMessageRepository.findBySupportTicketIdAndEntityStatusNotOrderByCreatedAtAsc(
                        ticketId, EntityStatus.DELETED)
                : supportTicketMessageRepository.findBySupportTicketIdAndEntityStatusNotAndVisibilityOrderByCreatedAtAsc(
                        ticketId, EntityStatus.DELETED, SupportTicketMessageVisibility.PUBLIC);
        return modelMapper.map(messages, new TypeToken<List<SupportTicketMessageDto>>() {}.getType());
    }

    public SupportTicketMessage addMessage(SupportTicket ticket,
                                           String authorUsername,
                                           SupportTicketMessageAuthorRole authorRole,
                                           SupportTicketMessageVisibility visibility,
                                           String body) {
        SupportTicketMessage message = new SupportTicketMessage();
        message.setSupportTicketId(ticket.getId());
        message.setAuthorUsername(authorUsername);
        message.setAuthorRole(authorRole);
        message.setVisibility(visibility != null ? visibility : SupportTicketMessageVisibility.PUBLIC);
        message.setBody(body.trim());
        message.setCreatedBy(authorUsername);
        message.setModifiedBy(authorUsername);
        return supportTicketMessageRepository.save(message);
    }

    public void addSystemMessage(SupportTicket ticket, String actorUsername, String body) {
        addMessage(ticket, actorUsername, SupportTicketMessageAuthorRole.SYSTEM,
                SupportTicketMessageVisibility.INTERNAL, body);
    }

    public void applyStatusTransition(SupportTicket ticket, SupportTicketStatus next, String actorUsername) {
        SupportTicketStatus previous = ticket.getStatus();
        ticket.setStatus(next);
        ticket.setModifiedBy(actorUsername);
        if (next == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(java.time.LocalDateTime.now());
        }
        if (next == SupportTicketStatus.CLOSED) {
            ticket.setClosedAt(java.time.LocalDateTime.now());
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(ticket.getClosedAt());
            }
        }
        if (next == SupportTicketStatus.OPEN && previous == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(null);
            ticket.setClosedAt(null);
        }
        supportTicketRepository.save(ticket);
        addSystemMessage(ticket, actorUsername,
                "Status changed from " + previous.name().replace('_', ' ')
                        + " to " + next.name().replace('_', ' ') + ".");
    }

    public void assignHandler(SupportTicket ticket, User handler, String actorUsername) {
        ticket.setAssignedHandlerUserId(handler.getId());
        ticket.setAssignedHandlerUsername(handler.getUsername());
        ticket.setModifiedBy(actorUsername);
        supportTicketRepository.save(ticket);
        addSystemMessage(ticket, actorUsername, "Assigned to " + handler.getUsername() + ".");
    }

    public Optional<User> resolveHandler(Long handlerUserId, String fallbackUsername) {
        if (handlerUserId != null) {
            return userRepository.findById(handlerUserId)
                    .filter(u -> u.getEntityStatus() != EntityStatus.DELETED);
        }
        if (fallbackUsername == null || fallbackUsername.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsernameAndEntityStatusNot(fallbackUsername.trim(), EntityStatus.DELETED);
    }

    public List<SupportTicket> filterForExport(SupportTicketExportFilterRequest filters) {
        List<SupportTicket> tickets = supportTicketRepository.findByEntityStatusNotOrderByCreatedAtDesc(EntityStatus.DELETED);
        if (filters == null) {
            return tickets;
        }
        String search = filters.getSearch() != null ? filters.getSearch().trim().toLowerCase(Locale.ROOT) : "";
        return tickets.stream()
                .filter(t -> filters.getStatus() == null || t.getStatus() == filters.getStatus())
                .filter(t -> {
                    if (search.isEmpty()) {
                        return true;
                    }
                    return (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase(Locale.ROOT).contains(search))
                            || (t.getSubject() != null && t.getSubject().toLowerCase(Locale.ROOT).contains(search))
                            || (t.getRequesterUsername() != null && t.getRequesterUsername().toLowerCase(Locale.ROOT).contains(search))
                            || (t.getAssignedHandlerUsername() != null
                            && t.getAssignedHandlerUsername().toLowerCase(Locale.ROOT).contains(search));
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public byte[] exportCsv(List<SupportTicket> tickets) {
        StringBuilder sb = new StringBuilder(String.join(",", EXPORT_HEADERS)).append('\n');
        for (SupportTicket ticket : tickets) {
            int messageCount = supportTicketMessageRepository
                    .findBySupportTicketIdAndEntityStatusNotOrderByCreatedAtAsc(ticket.getId(), EntityStatus.DELETED)
                    .size();
            sb.append(csv(ticket.getTicketNumber())).append(',')
                    .append(csv(ticket.getSubject())).append(',')
                    .append(csv(ticket.getStatus() != null ? ticket.getStatus().name() : "")).append(',')
                    .append(csv(ticket.getPriority() != null ? ticket.getPriority().name() : "")).append(',')
                    .append(csv(ticket.getCategory() != null ? ticket.getCategory().name() : "")).append(',')
                    .append(csv(ticket.getRequesterUsername())).append(',')
                    .append(csv(ticket.getAssignedHandlerUsername())).append(',')
                    .append(csv(formatTs(ticket.getCreatedAt()))).append(',')
                    .append(csv(formatTs(ticket.getResolvedAt()))).append(',')
                    .append(csv(formatTs(ticket.getClosedAt()))).append(',')
                    .append(messageCount).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportExcel(List<SupportTicket> tickets) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Support tickets");
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            int rowIdx = 1;
            for (SupportTicket ticket : tickets) {
                int messageCount = supportTicketMessageRepository
                        .findBySupportTicketIdAndEntityStatusNotOrderByCreatedAtAsc(ticket.getId(), EntityStatus.DELETED)
                        .size();
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(safe(ticket.getTicketNumber()));
                row.createCell(1).setCellValue(safe(ticket.getSubject()));
                row.createCell(2).setCellValue(ticket.getStatus() != null ? ticket.getStatus().name() : "");
                row.createCell(3).setCellValue(ticket.getPriority() != null ? ticket.getPriority().name() : "");
                row.createCell(4).setCellValue(ticket.getCategory() != null ? ticket.getCategory().name() : "");
                row.createCell(5).setCellValue(safe(ticket.getRequesterUsername()));
                row.createCell(6).setCellValue(safe(ticket.getAssignedHandlerUsername()));
                row.createCell(7).setCellValue(formatTs(ticket.getCreatedAt()));
                row.createCell(8).setCellValue(formatTs(ticket.getResolvedAt()));
                row.createCell(9).setCellValue(formatTs(ticket.getClosedAt()));
                row.createCell(10).setCellValue(messageCount);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportPdf(List<SupportTicket> tickets) throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (SupportTicket ticket : tickets.stream()
                .sorted(Comparator.comparing(SupportTicket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList()) {
            int messageCount = supportTicketMessageRepository
                    .findBySupportTicketIdAndEntityStatusNotOrderByCreatedAtAsc(ticket.getId(), EntityStatus.DELETED)
                    .size();
            rows.add(new String[]{
                    safe(ticket.getTicketNumber()),
                    safe(ticket.getSubject()),
                    ticket.getStatus() != null ? ticket.getStatus().name() : "",
                    ticket.getPriority() != null ? ticket.getPriority().name() : "",
                    safe(ticket.getRequesterUsername()),
                    safe(ticket.getAssignedHandlerUsername()),
                    formatTs(ticket.getCreatedAt()),
                    formatTs(ticket.getClosedAt()),
                    String.valueOf(messageCount)
            });
        }
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title("Help & Support queue")
                .reportCode("HLP-SUP")
                .subtitle("Support ticket export")
                .columnHeaders(new String[]{
                        "Ticket", "Subject", "Status", "Priority", "Requester", "Handler", "Opened", "Closed", "Msgs"
                })
                .rows(rows)
                .landscape(true)
                .build());
    }

    private static String formatTs(java.time.LocalDateTime value) {
        return value == null ? "" : EXPORT_TS.format(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
