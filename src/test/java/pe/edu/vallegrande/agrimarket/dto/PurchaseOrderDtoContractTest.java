package pe.edu.vallegrande.agrimarket.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderDtoContractTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void purchaseOrderRequestsKeepTheirFlatJsonShape() throws Exception {
        String payload = """
                {
                  "purchaseOrderDate": "2026-11-19T13:00:00",
                  "status": "PENDING",
                  "totalAmount": 3.50,
                  "supplierId": "9",
                  "storeId": 2,
                  "notes": "Restock",
                  "items": [{"quantity": 1, "unitPrice": 3.50, "productId": "13"}]
                }
                """;

        CreatePurchaseOrderRequest createRequest =
                jsonMapper.readValue(payload, CreatePurchaseOrderRequest.class);
        UpdatePurchaseOrderRequest updateRequest =
                jsonMapper.readValue(payload, UpdatePurchaseOrderRequest.class);

        assertEquals("9", createRequest.getSupplierId());
        assertEquals("13", createRequest.getItems().get(0).productId());
        assertEquals("Restock", updateRequest.getNotes());

        JsonNode serializedRequest = jsonMapper.valueToTree(createRequest);
        assertTrue(serializedRequest.has("purchaseOrderDate"));
        assertTrue(serializedRequest.has("items"));
        assertFalse(serializedRequest.has("details"));
    }

    @Test
    void storefrontProductKeepsTheAvailableJsonProperty() {
        StorefrontProductDTO product = new StorefrontProductDTO();
        product.setAvailable(true);

        JsonNode serializedProduct = jsonMapper.valueToTree(product);

        assertTrue(serializedProduct.path("available").asBoolean());
        assertFalse(serializedProduct.has("isAvailable"));
    }
}
