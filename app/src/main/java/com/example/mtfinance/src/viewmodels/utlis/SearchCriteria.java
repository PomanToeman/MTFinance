package com.example.mtfinance.src.viewmodels.utlis;

import androidx.annotation.NonNull;

import com.example.mtfinance.src.trackingengine.TrackingType;

/**
 * To encompass all search criteria for when search a transaction or category.
 */
public class SearchCriteria {
    private String query;

    private TrackingType typeFilter;


    public SearchCriteria(String query, TrackingType typeFilter) {
        this.query = query;
        this.typeFilter = typeFilter;
    }

    public void setQuery(String query) {
        if (query != null && query.endsWith(" ") && !query.startsWith(" ")) {

            this.query = query;

        }
        else {
            this.query = query != null ? query.trim() : "";
        }
    }

    public void setTypeFilter(TrackingType typeFilter) {
        this.typeFilter = typeFilter;
    }


    public String getQuery() {
        return query;
    }
    public String getTrimmedQuery() {
        return query != null ? query.trim() : "";
    }

    public TrackingType getTypeFilter() {
        return typeFilter;
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: " + query + ", Type: " + typeFilter;
    }



}
