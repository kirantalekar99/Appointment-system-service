package com.appointmentsystem.config;

import com.appointmentsystem.model.Appointment;
import com.appointmentsystem.model.NotificationLog;
import com.appointmentsystem.model.SequencedDocument;
import com.appointmentsystem.service.MongoSequenceGenerator;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MongoSequencedDocumentListener {

    private final MongoSequenceGenerator sequenceGenerator;

    public MongoSequencedDocumentListener(MongoSequenceGenerator sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @EventListener
    public void beforeConvert(BeforeConvertEvent<Object> event) {
        Object source = event.getSource();

        if (source instanceof SequencedDocument document && document.getId() == null) {
            document.setId(sequenceGenerator.nextId(source.getClass().getSimpleName()));
        }

        if (source instanceof Appointment appointment) {
            appointment.syncLegacyColumns();
        }

        if (source instanceof NotificationLog notificationLog && notificationLog.getCreatedAt() == null) {
            notificationLog.setCreatedAt(LocalDateTime.now());
        }
    }
}
