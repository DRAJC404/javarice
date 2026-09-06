package Services;

import java.util.ArrayList;
import java.util.List;

public class ProductService {

    public List<String> getProducts() {

        List<String> products = new ArrayList<>();

        products.add("Coffee");
        products.add("Milk");
        products.add("Bread");
        products.add("Instant Noodles");
        products.add("Soft Drinks");

        return products;
    }

    public int getProductCount() {
        return getProducts().size();
    }

    public int getInventoryCount() {
        return 125;
    }

    public double getTotalSales() {
        return 15250.00;
    }
}