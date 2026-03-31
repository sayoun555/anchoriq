package com.anchoriq.api.application.notification;

import com.anchoriq.api.dto.request.notification.NotificationRuleRequest;
import com.anchoriq.api.dto.request.notification.NotificationTestRequest;
import com.anchoriq.api.dto.response.notification.NotificationHistoryResponse;
import com.anchoriq.api.dto.response.notification.NotificationRuleResponse;
import com.anchoriq.api.dto.response.notification.NotificationSettingsResponse;
import com.anchoriq.automation.notification.NotificationDispatcher;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.operation.notification.model.NotificationChannel;
import com.anchoriq.core.domain.operation.notification.model.NotificationRule;
import com.anchoriq.core.domain.operation.notification.repository.NotificationHistoryRepository;
import com.anchoriq.core.domain.operation.notification.repository.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class NotificationApplicationServiceImpl implements NotificationApplicationService {

    private final NotificationRuleRepository ruleRepository;
    private final NotificationHistoryRepository historyRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    @Transactional
    public NotificationRuleResponse createRule(Long userId, NotificationRuleRequest request) {
        NotificationChannel channel = NotificationChannel.valueOf(request.getChannel().toUpperCase());
        NotificationRule rule = NotificationRule.create(
                userId,
                request.getName(),
                request.getConditionType(),
                request.getConditionValue(),
                channel
        );
        NotificationRule saved = ruleRepository.save(rule);
        return NotificationRuleResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationRuleResponse> getRules(Long userId, Pageable pageable) {
        return ruleRepository.findByUserId(userId, pageable)
                .map(NotificationRuleResponse::from);
    }

    @Override
    @Transactional
    public NotificationRuleResponse updateRule(Long userId, Long ruleId, NotificationRuleRequest request) {
        NotificationRule rule = findRuleOwnedBy(userId, ruleId);
        NotificationChannel channel = NotificationChannel.valueOf(request.getChannel().toUpperCase());
        rule.update(request.getName(), request.getConditionType(), request.getConditionValue(), channel);
        NotificationRule saved = ruleRepository.save(rule);
        return NotificationRuleResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        NotificationRule rule = findRuleOwnedBy(userId, ruleId);
        ruleRepository.deleteById(rule.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationHistoryResponse> getHistory(Pageable pageable) {
        return historyRepository.findAll(pageable)
                .map(NotificationHistoryResponse::from);
    }

    @Override
    public boolean sendTestNotification(NotificationTestRequest request) {
        String message = request.getMessage() != null ? request.getMessage()
                : "This is a test notification from AnchorIQ";
        return notificationDispatcher.sendTest(
                request.getChannel(), request.getDestination(), message);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(Long userId) {
        List<NotificationRule> activeRules = ruleRepository.findActiveRulesByUserId(userId);

        boolean slackEnabled = activeRules.stream()
                .anyMatch(rule -> rule.getChannel() == NotificationChannel.SLACK);
        boolean emailEnabled = activeRules.stream()
                .anyMatch(rule -> rule.getChannel() == NotificationChannel.EMAIL);

        return NotificationSettingsResponse.builder()
                .slackEnabled(slackEnabled)
                .emailEnabled(emailEnabled)
                .activeRuleCount(activeRules.size())
                .build();
    }

    private NotificationRule findRuleOwnedBy(Long userId, Long ruleId) {
        NotificationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("NotificationRule", ruleId.toString()));
        if (!rule.isOwnedBy(userId)) {
            throw new EntityNotFoundException("NotificationRule", ruleId.toString());
        }
        return rule;
    }
}
