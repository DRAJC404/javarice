package Services;

import java.util.ArrayList;
import java.util.List;

public class ProductService {

    public List<String> getProducts() {

        List<String> products = new ArrayList<>();

        products.add("Coffee - 50");
        products.add("Bread - 35");
        products.add("Milk - 80");
        products.add("Instant Noodles - 15");

        return products;
    }
}