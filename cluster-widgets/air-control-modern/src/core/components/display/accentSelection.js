import { getState, setState, subscribe } from '../../state.js';
import { div, span, img } from '../../../utils/createElement.js';

const ACCENT_META = [
    { id: 'accent_blue', value: 'blue', label: 'Azul' },
    { id: 'accent_yellow', value: 'yellow', label: 'Amarelo' },
    { id: 'accent_red', value: 'red', label: 'Vermelho' },
    { id: 'accent_orange', value: 'orange', label: 'Laranja' },
];

function accentIcon(fill) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 48 48" fill="none"><circle cx="24" cy="24" r="14" fill="${fill}" stroke="rgba(255,255,255,0.35)" stroke-width="2"/></svg>`;
    return `data:image/svg+xml;base64,${btoa(svg)}`;
}

export function createAccentSelectionScreen() {
    const main = document.createElement('main');
    main.className = 'main-theme-container';

    const carousel = div({ className: 'menu-carousel' });
    main.appendChild(carousel);

    const menuItemsData = [
        { id: 'accent_title', label: 'Cor do layout', type: 'title' },
        ...ACCENT_META.map((a) => ({
            id: a.id,
            label: a.label,
            type: 'accent',
            value: a.value,
            iconSrc: accentIcon(
                a.value === 'blue' ? '#3b82f6'
                    : a.value === 'yellow' ? '#eab308'
                        : a.value === 'red' ? '#ef4444' : '#f97316'),
        })),
    ];

    const itemElements = {};

    menuItemsData.forEach((itemData, index) => {
        const children = [];
        if (itemData.type === 'accent') {
            children.push(div({
                className: 'icon-container',
                children: [img({ className: 'menu-icon', src: itemData.iconSrc, alt: itemData.label })],
            }));
        }

        const labelText = span({ className: 'item-label-text', style: { display: 'inline-block' } }, itemData.label);

        const checkIcon = div({
            className: 'check-icon',
            style: {
                marginLeft: 'auto',
                color: '#FFFFFF',
                display: 'flex',
                alignItems: 'center',
                opacity: '0',
                transform: 'scale(0.5)',
                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
            },
        });
        checkIcon.innerHTML = '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>';

        const labelContainer = div({
            className: 'menu-label',
            style: {
                display: 'flex',
                alignItems: 'center',
                flexGrow: '1',
                transition: 'all 0.3s ease',
            },
        }, labelText, checkIcon);

        children.push(labelContainer);

        const itemEl = div({
            id: itemData.id,
            className: `menu-item ${itemData.type === 'title' ? 'title-item' : ''}`,
            'data-index': String(index),
        }, ...children);

        if (itemData.type === 'title') {
            itemEl.style.pointerEvents = 'none';
            itemEl.style.opacity = '0.6';
            itemEl.style.fontSize = '0.7em';
            itemEl.style.textTransform = 'uppercase';
            itemEl.style.letterSpacing = '1.5px';
            itemEl.style.marginBottom = '10px';
            itemEl.style.marginTop = '10px';
            itemEl.style.border = 'none';
            itemEl.style.background = 'none';
            itemEl.style.paddingLeft = '15px';
            itemEl.style.display = 'flex';
            itemEl.style.alignItems = 'center';
        }

        carousel.appendChild(itemEl);
        itemElements[itemData.id] = { element: itemEl, labelContainer, checkIcon };
    });

    const updateItems = () => {
        const focusedId = getState('accentFocus') || 'accent_blue';
        const currentIndex = menuItemsData.findIndex((item) => item.id === focusedId);
        const currentAccent = getState('uiAccent') || 'blue';

        menuItemsData.forEach((itemData) => {
            const cache = itemElements[itemData.id];
            if (!cache) return;
            const isFocused = itemData.id === focusedId;
            const isSelected = itemData.type === 'accent' && itemData.value === currentAccent;

            cache.element.className = `menu-item ${isFocused ? 'focused' : ''} ${itemData.type === 'title' ? 'title-item' : ''}`;

            if (itemData.type === 'accent') {
                cache.labelContainer.style.color = isSelected ? 'var(--text-main, #FFFFFF)' : 'var(--text-cold-gray, #B0B8C4)';
                cache.labelContainer.style.fontWeight = isSelected ? 'bold' : 'normal';
                if (cache.checkIcon) {
                    cache.checkIcon.style.opacity = isSelected ? '1' : '0';
                    cache.checkIcon.style.transform = isSelected ? 'scale(1)' : 'scale(0.5)';
                }
            }
        });

        carousel.className = `menu-carousel focus-${currentIndex >= 0 ? currentIndex : 0}`;
    };

    updateItems();
    const subscriptions = [
        subscribe('accentFocus', updateItems),
        subscribe('uiAccent', updateItems),
    ];

    const cleanup = () => {
        subscriptions.forEach((u) => u());
    };

    return { element: main, cleanup };
}
