package io.github.isagroup.services.yaml;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.github.isagroup.models.AddOn;
import io.github.isagroup.models.Feature;
import io.github.isagroup.models.Plan;
import io.github.isagroup.models.PricingManager;
import io.github.isagroup.models.UsageLimit;

public class PricingManagerSerializer {

    private PricingManager pricingManager;
    private Map<String, Object> serializedPricingManager;

    public PricingManagerSerializer(PricingManager pricingManager) {
        this.pricingManager = pricingManager;
        this.serializedPricingManager = new LinkedHashMap<>();
    }

    private void initPricingManagerMetadata() {
        serializedPricingManager.put("saasName", pricingManager.getSaasName());
        serializedPricingManager.put("day", pricingManager.getDay());
        serializedPricingManager.put("month", pricingManager.getMonth());
        serializedPricingManager.put("year", pricingManager.getYear());
        serializedPricingManager.put("currency", pricingManager.getCurrency());
        serializedPricingManager.put("hasAnnualPayment", pricingManager.getHasAnnualPayment());
    }

    public Map<String, Object> serialize() throws Exception {

        initPricingManagerMetadata();

        if (pricingManager.getFeatures() == null) {
            throw new Exception(
                    "Currently you have not defined any features. You may dump a config until you define them");
        }

        if (pricingManager.getUsageLimits() == null) {
            throw new Exception(
                    "Currently you have not defined any usage limits. You may dump a config until you define them");
        }

        serializedPricingManager.put("features", serializeFeatures());
        serializedPricingManager.put("usageLimits", serializeUsageLimits());
        serializedPricingManager.put("plans", serializePlans());

        if (pricingManager.getAddOns() != null) {
            serializedPricingManager.put("addOns", AddOn.serializeAddOns(pricingManager.getAddOns()));
        }
        return serializedPricingManager;
    }

    private Map<String, Object> serializeFeatures() {
        Map<String, Object> serializedFeatures = new LinkedHashMap<>();
        for (Entry<String, Feature> entry : pricingManager.getFeatures().entrySet()) {
            serializedFeatures.put(entry.getKey(), entry.getValue().serializeFeature());

        }
        return serializedFeatures;
    }

    private Map<String, Object> serializeUsageLimits() {
        Map<String, Object> serializedUsageLimits = new LinkedHashMap<>();

        if (pricingManager.getUsageLimits() == null) {
            return null;
        }

        for (Entry<String, UsageLimit> entry : pricingManager.getUsageLimits().entrySet()) {
            serializedUsageLimits.put(entry.getKey(), entry.getValue().serializeUsageLimit());

        }

        return serializedUsageLimits;

    }

    private Map<String, Object> serializePlans() {
        Map<String, Object> serializedPlans = new LinkedHashMap<>();
        for (Plan plan : pricingManager.getPlans().values()) {
            serializedPlans.put(plan.getName(), plan.serializePlan());
        }
        return serializedPlans;
    }
}
