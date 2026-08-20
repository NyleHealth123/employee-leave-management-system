package com.example.leavemanagement.integration;

import com.example.leavemanagement.audit.application.AuditQueryService;
import com.example.leavemanagement.audit.persistence.AuditEventEntity;
import com.example.leavemanagement.audit.persistence.AuditEventRepository;
import com.example.leavemanagement.shared.api.DomainException;
import com.example.leavemanagement.shared.security.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.*;

class AuditHistoryQueryTest {
    private final AuditEventRepository events = mock(AuditEventRepository.class); private final CurrentActorProvider actors = mock(CurrentActorProvider.class); private final ObjectMapper mapper = new ObjectMapper(); private final UUID actorId = UUID.randomUUID();
    @Test void administratorGetsStablePageWithApprovedFields() { when(actors.require()).thenReturn(new CurrentActor(actorId, UUID.randomUUID(), null, Set.of("ADMINISTRATOR"), "Admin")); var e=AuditEventEntity.decision(actorId,UUID.randomUUID(),"LEAVE_APPROVED","PENDING","APPROVED","approved"); when(events.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(e),PageRequest.of(0,20),1)); var page=new AuditQueryService(events,actors,mapper).list(0,20,null,null); assertThat(page.content()).hasSize(1); assertThat(page.content().getFirst().reason()).isEqualTo("approved"); verify(events).findAll(argThat((Pageable p) -> p.getSort().getOrderFor("occurredAt").getDirection()==Sort.Direction.DESC)); }
    @Test void nonAdministratorIsDeniedBeforeQuery() { when(actors.require()).thenReturn(new CurrentActor(actorId, UUID.randomUUID(), null, Set.of("MANAGER"), "Manager")); assertThatThrownBy(() -> new AuditQueryService(events,actors,mapper).list(0,20,null,null)).isInstanceOf(DomainException.class).hasMessageContaining("Administrator"); verifyNoInteractions(events); }
    @Test void entityFiltersUseReadOnlyQueryAndNeverRewriteHistory() { when(actors.require()).thenReturn(new CurrentActor(actorId, UUID.randomUUID(), null, Set.of("ADMINISTRATOR"), "Admin")); var entityId=UUID.randomUUID(); when(events.search(eq("LEAVE_REQUEST"),eq(entityId),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(),PageRequest.of(0,10),0)); assertThat(new AuditQueryService(events,actors,mapper).list(0,10,"LEAVE_REQUEST",entityId).content()).isEmpty(); verify(events).search(eq("LEAVE_REQUEST"),eq(entityId),any(Pageable.class)); verify(events,never()).save(any()); verify(events,never()).delete(any()); }
}
