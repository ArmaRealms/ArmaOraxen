package io.th0rgal.oraxen;

import io.th0rgal.oraxen.pack.upload.UploadManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OraxenPluginTest {

    @Test
    void setUploadManagerUnregistersPreviousManagerWhenReplacing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        UploadManager previous = mock(UploadManager.class);
        UploadManager replacement = mock(UploadManager.class);

        plugin.setUploadManager(previous);
        doAnswer(invocation -> {
            assertSame(previous, plugin.getUploadManager());
            return null;
        }).when(previous).unregister();

        plugin.setUploadManager(replacement);

        verify(previous).unregister();
        verify(replacement, never()).unregister();
        assertSame(replacement, plugin.getUploadManager());
    }

    @Test
    void setUploadManagerDoesNotUnregisterSameManagerInstance() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        UploadManager manager = mock(UploadManager.class);

        plugin.setUploadManager(manager);
        clearInvocations(manager);
        plugin.setUploadManager(manager);

        verify(manager, never()).unregister();
        assertSame(manager, plugin.getUploadManager());
    }

    @Test
    void setUploadManagerUnregistersPreviousManagerWhenClearing() {
        OraxenPlugin plugin = mock(OraxenPlugin.class, CALLS_REAL_METHODS);
        UploadManager previous = mock(UploadManager.class);

        plugin.setUploadManager(previous);
        clearInvocations(previous);
        plugin.setUploadManager(null);

        verify(previous).unregister();
        assertNull(plugin.getUploadManager());
    }
}
