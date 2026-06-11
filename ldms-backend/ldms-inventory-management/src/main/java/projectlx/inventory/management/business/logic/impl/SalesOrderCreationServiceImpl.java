package projectlx.inventory.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projectlx.inventory.management.business.logic.api.SalesOrderCreationService;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.PurchaseOrderLine;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.inventory.management.model.SalesOrder;
import projectlx.inventory.management.model.SalesOrderLine;
import projectlx.inventory.management.model.SalesOrderStatus;
import projectlx.inventory.management.repository.SalesOrderRepository;
import projectlx.inventory.management.utils.NumberGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Service responsible for creating Sales Orders from approved, payment-confirmed Purchase Orders.
 *
 * PAYMENT-GATED FULFILLMENT FLOW:
 * 1. PO gets APPROVED via multi-stage workflow (customer + supplier stages)
 * 2. Billing service creates an invoice from po.approved event
 * 3. Customer pays invoice → billing.payment.verified event published
 * 4. PaymentVerifiedEventHandler calls this service → SO created in PENDING_APPROVAL
 * 5. SO approval (multi-stage) → APPROVED
 * 6. Stock is reserved at shipment start (APPROVED → PARTIALLY_SHIPPED/SHIPPED)
 *
 * KEY RULE: SO starts in PENDING_APPROVAL because payment has already been confirmed.
 * The SO must go through its own approval before goods are dispatched.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalesOrderCreationServiceImpl implements SalesOrderCreationService {

    private final SalesOrderRepository salesOrderRepository;
    private final NumberGenerator numberGenerator;

    /**
     * Creates a Sales Order from an approved Purchase Order
     * 
     * @param purchaseOrder The approved PO
     * @param supplierOrgId The supplier organization ID
     * @param createdByUserId User creating the SO
     * @param locale User locale
     * @return The created Sales Order in PENDING status
     */
    public SalesOrder createFromPurchaseOrder(PurchaseOrder purchaseOrder, 
                                             Long supplierOrgId,
                                             Long createdByUserId,
                                             Locale locale) {
        
        log.info("Creating Sales Order from approved Purchase Order: {}", purchaseOrder.getId());
        
        // Validate PO is approved
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot create SO from PO %d - status is %s, expected APPROVED",
                    purchaseOrder.getId(), purchaseOrder.getStatus()));
        }
        
        // Check if SO already exists for this PO
        if (salesOrderRepository.findByPurchaseOrderId(purchaseOrder.getId()).isPresent()) {
            throw new IllegalStateException(
                String.format("Sales Order already exists for Purchase Order %d", 
                    purchaseOrder.getId()));
        }
        
        // Create Sales Order
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setSalesOrderNumber(numberGenerator.generateSalesOrderNumber());
        salesOrder.setPurchaseOrderId(purchaseOrder.getId());
        salesOrder.setPurchaseOrderNumber(purchaseOrder.getPurchaseOrderNumber());
        salesOrder.setSupplierOrganizationId(supplierOrgId);
        salesOrder.setCustomerId(purchaseOrder.getOrganizationId());
        
        // Copy dates and financial details
        salesOrder.setOrderDate(purchaseOrder.getOrderDate());
        salesOrder.setExpectedDeliveryDate(purchaseOrder.getExpectedDate());
        salesOrder.setTotalAmount(purchaseOrder.getTotalAmount());
        salesOrder.setPaymentTerm(purchaseOrder.getPaymentTerm());
        
        // Copy notes
        salesOrder.setNotes(buildSalesOrderNotes(purchaseOrder));
        
        // Set receiving warehouse as fulfillment warehouse (if specified)
        if (purchaseOrder.getReceivingWarehouseId() != null) {
            salesOrder.setFulfillmentWarehouseId(purchaseOrder.getReceivingWarehouseId());
        }
        
        // Set audit fields
        salesOrder.setCreatedByUserId(createdByUserId);
        salesOrder.setUpdatedByUserId(createdByUserId);

        // Initialize entity (sets default status)
        salesOrder.create();

        // Default to PENDING_APPROVAL: Payment has been confirmed by billing before SO creation.
        // The SO goes through an approval flow before goods are dispatched.
        salesOrder.setStatus(SalesOrderStatus.PENDING_APPROVAL);
        
        // Create Sales Order Lines from Purchase Order Lines
        List<SalesOrderLine> salesOrderLines = createSalesOrderLines(
            purchaseOrder, salesOrder, createdByUserId);
        salesOrder.getSalesOrderLines().addAll(salesOrderLines);
        
        // Save
        SalesOrder savedSalesOrder = salesOrderRepository.save(salesOrder);
        
        log.info("Created Sales Order {} (PENDING_APPROVAL) from Purchase Order {} - awaiting SO approval",
            savedSalesOrder.getSalesOrderNumber(), purchaseOrder.getPurchaseOrderNumber());
        
        return savedSalesOrder;
    }
    
    /**
     * Creates Sales Order Lines from Purchase Order Lines
     */
    private List<SalesOrderLine> createSalesOrderLines(PurchaseOrder purchaseOrder,
                                                      SalesOrder salesOrder,
                                                      Long createdByUserId) {
        
        List<SalesOrderLine> salesOrderLines = new ArrayList<>();
        
        for (PurchaseOrderLine poLine : purchaseOrder.getPurchaseOrderLines()) {
            SalesOrderLine soLine = new SalesOrderLine();
            soLine.setSalesOrder(salesOrder);
            soLine.setProduct(poLine.getProduct());
            soLine.setQuantity(poLine.getQuantity());
            soLine.setUnitPrice(poLine.getUnitPrice());
            soLine.setTotalPrice(poLine.getTotalPrice());
            soLine.setUnitOfMeasure(poLine.getUnitOfMeasure());
            soLine.setFulfilledQuantity(BigDecimal.ZERO);
            soLine.setCreatedByUserId(createdByUserId);
            soLine.setUpdatedByUserId(createdByUserId);
            soLine.create();
            
            salesOrderLines.add(soLine);
        }
        
        return salesOrderLines;
    }
    
    /**
     * Builds SO notes from PO details
     */
    private String buildSalesOrderNotes(PurchaseOrder purchaseOrder) {
        StringBuilder notes = new StringBuilder();
        notes.append("Created from Purchase Order: ").append(purchaseOrder.getPurchaseOrderNumber());
        
        if (purchaseOrder.getNotes() != null && !purchaseOrder.getNotes().isBlank()) {
            notes.append("\n\nCustomer Notes:\n").append(purchaseOrder.getNotes());
        }
        
        // Add import/export info if present
        if (purchaseOrder.getIsImport() != null && purchaseOrder.getIsImport()) {
            if (purchaseOrder.getCustomsDeclarationNumber() != null) {
                notes.append("\n\nCustoms Declaration: ")
                        .append(purchaseOrder.getCustomsDeclarationNumber());
            }
            if (purchaseOrder.getPortOfEntry() != null) {
                notes.append("\nPort of Entry: ")
                        .append(purchaseOrder.getPortOfEntry());
            }
        }
        
        return notes.toString();
    }
}
