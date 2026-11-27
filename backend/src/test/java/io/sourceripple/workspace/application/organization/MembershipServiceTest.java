package io.sourceripple.workspace.application.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.sourceripple.identity.application.access.AccessPolicy;
import io.sourceripple.identity.application.account.UserAccountService;
import io.sourceripple.identity.domain.User;
import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.status.ConflictException;
import io.sourceripple.shared.api.error.status.ForbiddenException;
import io.sourceripple.shared.api.error.status.NotFoundException;
import io.sourceripple.workspace.api.dto.organization.AddMemberRequest;
import io.sourceripple.workspace.domain.MemberRow;
import io.sourceripple.workspace.domain.Organization;
import io.sourceripple.workspace.domain.OrganizationRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Mock
    private OrganizationRepository organizations;

    @Mock
    private UserAccountService accounts;

    @Mock
    private AccessPolicy access;

    @Mock
    private EntityManager entityManager;

    private MembershipService service;
    private Organization organization;
    private User member;

    @BeforeEach
    void setUp() {
        service = new MembershipService(organizations, accounts, access, entityManager);
        User owner = user(OWNER_ID);
        member = user(MEMBER_ID);
        organization = new Organization(owner, "Acme", Instant.now());
        organization.addMember(owner);
    }

    @Test
    void ownerAddsAnExistingAccountByEmail() {
        MemberRow added = row(MEMBER_ID);
        when(accounts.findIdByEmail("ada@example.com")).thenReturn(Optional.of(MEMBER_ID));
        when(organizations.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(entityManager.getReference(User.class, MEMBER_ID)).thenReturn(member);
        when(organizations.findMember(ORGANIZATION_ID, MEMBER_ID)).thenReturn(Optional.of(added));

        assertThat(service.add(ORGANIZATION_ID, new AddMemberRequest("ada@example.com")).userId())
                .isEqualTo(MEMBER_ID);
        assertThat(organization.getMembers()).contains(member);
    }

    @Test
    void addingSomeoneWhoIsAlreadyAMemberIsAConflict() {
        when(accounts.findIdByEmail("ada@example.com")).thenReturn(Optional.of(MEMBER_ID));
        when(organizations.existsByIdAndMembersId(ORGANIZATION_ID, MEMBER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.add(ORGANIZATION_ID, new AddMemberRequest("ada@example.com")))
                .isInstanceOfSatisfying(ConflictException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_TRANSITION));
        verify(organizations, never()).flush();
    }

    @Test
    void nonOwnerCannotAddOrRemove() {
        doThrow(new ForbiddenException("owner only")).when(access).requireOwner(ORGANIZATION_ID);

        assertThatThrownBy(() -> service.add(ORGANIZATION_ID, new AddMemberRequest("ada@example.com")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.remove(ORGANIZATION_ID, MEMBER_ID)).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(accounts, organizations);
    }

    @Test
    void addingAnUnknownEmailIsNotFound() {
        when(accounts.findIdByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(ORGANIZATION_ID, new AddMemberRequest("nobody@example.com")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void theOwnerCannotBeRemoved() {
        when(organizations.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));

        assertThatThrownBy(() -> service.remove(ORGANIZATION_ID, OWNER_ID)).isInstanceOf(ConflictException.class);
        assertThat(organization.getMembers()).hasSize(1);
    }

    @Test
    void ownerRemovesAMember() {
        organization.addMember(member);
        when(organizations.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));

        service.remove(ORGANIZATION_ID, MEMBER_ID);

        assertThat(organization.getMembers()).extracting(User::getId).containsExactly(OWNER_ID);
    }

    private static User user(UUID id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private static MemberRow row(UUID userId) {
        MemberRow row = mock(MemberRow.class);
        when(row.getUserId()).thenReturn(userId);
        return row;
    }
}
