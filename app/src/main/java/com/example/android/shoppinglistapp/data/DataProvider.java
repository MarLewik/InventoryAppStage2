package com.example.android.shoppinglistapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.shoppinglistapp.R;

import static com.example.android.shoppinglistapp.data.DataContract.CONTENT_AUTHORITY;
import static com.example.android.shoppinglistapp.data.DataContract.PATH_PRODUCTS;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_IMAGE;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_NAME;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_PRICE;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_PRODUCT_QUANTITY;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_SUPPLIERNAME;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.COLUMN_SUPPLIERPHONENUMBER;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry.TABLE_NAME;
import static com.example.android.shoppinglistapp.data.DataContract.ProductEntry._ID;

/**
 * Created by Marcin on 10/08/2018.
 */
public class DataProvider extends ContentProvider {

    public static final String LOG_TAG = DataProvider.class.getSimpleName();
    private static final int ITEMS = 100;
    private static final int ITEM_ID = 101;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PRODUCTS, ITEMS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PRODUCTS + "/#", ITEM_ID);
    }

    /**
     * Database helper object
     */
    private DataDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new DataDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                cursor = database.query(TABLE_NAME, projection, null, null, null, null, sortOrder);
                break;
            case ITEM_ID:
                selection = _ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException((R.string.cannot_query_unknown_URI) + uri.toString());
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }


    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return DataContract.ProductEntry.CONTENT_LIST_TYPE;
            case ITEM_ID:
                return DataContract.ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException(R.string.unknown_URI + uri.toString() + R.string.with_match + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return insertProduct(uri, contentValues);
            default:
                throw new IllegalArgumentException(R.string.insertion_is_not_supported_for + uri.toString());
        }
    }

    private Uri insertProduct(Uri uri, ContentValues values) {

        String name = values.getAsString(COLUMN_PRODUCT_NAME);
        if (name == null) {
            throw new IllegalArgumentException(String.valueOf(R.string.product_requires_a_product_name));
        }

        String suppliername = values.getAsString(COLUMN_SUPPLIERNAME);
        if (suppliername == null) {
            throw new IllegalArgumentException(String.valueOf(R.string.product_requires_a_product_name));
        }

        String supplierphonenumber = values.getAsString(COLUMN_SUPPLIERPHONENUMBER);
        if (supplierphonenumber == null) {
            throw new IllegalArgumentException(String.valueOf(R.string.product_requires_a_product_name));
        }

        Integer quantity = values.getAsInteger(COLUMN_PRODUCT_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException(String.valueOf(R.string.product_requires_valid_quantity));
        }

        Integer price = values.getAsInteger(COLUMN_PRODUCT_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException(String.valueOf(R.string.product_requires_valid_price));
        }


        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(TABLE_NAME, null, values);
        if (id == -1) {
            Log.e(String.valueOf(R.string.product_provider),R.string.failed_to_insert_row_for + uri.toString());
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                rowsDeleted = database.delete(TABLE_NAME, selection, selectionArgs);
                break;
            case ITEM_ID:
                selection = _ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(R.string.deletion_is_not_supported_for + uri.toString());
        }
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ITEMS:
                return updateProduct(uri, values, selection, selectionArgs);
            case ITEM_ID:
                selection = _ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException(R.string.update_is_not_supported_for + uri.toString());
        }
    }

    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.size() == 0) {
            return 0;
        }
        if (values.containsKey(COLUMN_PRODUCT_NAME)) {
            String name = values.getAsString(COLUMN_PRODUCT_NAME);
            if (name == null) {
                throw new IllegalArgumentException(String.valueOf(R.string.product_requires_a_product_name));
            }
        }

        if (values.containsKey(COLUMN_SUPPLIERNAME)) {
            String name = values.getAsString(COLUMN_SUPPLIERNAME);
            if (name == null) {
                throw new IllegalArgumentException(String.valueOf(R.string.product_requires_a_product_name));
            }
        }

        if (values.containsKey(COLUMN_SUPPLIERPHONENUMBER)) {
            String name = values.getAsString(COLUMN_SUPPLIERPHONENUMBER);
            if (name == null) {
                throw new IllegalArgumentException(String.valueOf(R.string.product_requires_a_product_name));
            }
        }

        Integer quantity = values.getAsInteger(COLUMN_PRODUCT_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException(String.valueOf(R.string.product_requires_valid_quantity));
        }
        if (values.containsKey(COLUMN_PRODUCT_PRICE)) {
            Integer price = values.getAsInteger(COLUMN_PRODUCT_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException(String.valueOf(R.string.product_requires_valid_price));
            }
        }
        if (values.containsKey(COLUMN_PRODUCT_IMAGE)) {
            String productImage = values.getAsString(COLUMN_PRODUCT_IMAGE);
            if (productImage == null) {
                throw new IllegalArgumentException(String.valueOf(R.string.product_requires_an_image));
            }
        }

        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        int rowsUpdated = database.update(TABLE_NAME, values, selection, selectionArgs);
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}

