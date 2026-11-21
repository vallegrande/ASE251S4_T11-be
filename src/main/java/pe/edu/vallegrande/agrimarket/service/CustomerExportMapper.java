package pe.edu.vallegrande.agrimarket.service;

import pe.edu.vallegrande.agrimarket.model.mongo.Customer;
import pe.edu.vallegrande.agrimarket.model.mongo.GeographicDivision;
import org.springframework.stereotype.Component;

@Component
public class CustomerExportMapper {

    private static final String[] EXPORT_HEADERS = {
            "id", "docType", "docNumber", "firstName", "lastName", "email",
            "phone", "address", "department", "province", "district", "customerSince", "isActive"
    };

    public String[] headers() {
        return EXPORT_HEADERS.clone();
    }

    public String[] toRow(Customer customer) {
        GeographicDivision division = customer.getUbigeo();
        return new String[] {
                text(customer.getId()),
                text(customer.getDocType()),
                text(customer.getDocNumber()),
                text(customer.getFirstName()),
                text(customer.getLastName()),
                text(customer.getEmail()),
                text(customer.getPhone()),
                text(customer.getAddress()),
                division != null ? text(division.getDepartment()) : "",
                division != null ? text(division.getProvince()) : "",
                division != null ? text(division.getDistrict()) : "",
                text(customer.getCustomerSince()),
                text(customer.getIsActive())
        };
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
