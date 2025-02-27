import React, { useState, useRef, useEffect, useContext } from 'react';
import { useSelector } from 'react-redux';
import { animated, useSpring, useTransition } from 'react-spring';

import { useOutsideClick } from 'hooks/useOutsideClick';
import { ModalContext } from 'components/common/pop-up';
import { getTranslatedText } from 'components/local/localisation';

import { categoryImages } from './config';

import {
  Image,
  DropItems,
  SelectItem,
  WrapSelect,
  PlaceHolder,
  SelectLabel,
  SelectTitle,
  AnimatedLabel,
} from './styles';

const ShowSelectItem = ({
  data,
  text,
  value,
  onClick,
  typeError,
  titleError,
  placeholder,
  showImg = false,
  overflows = false,
  categories = false,
}) => {
  const { openModal } = useContext(ModalContext);
  const { lang } = useSelector((state) => state.auth);
  const [opened, setOpened] = useState(false);

  useEffect(() => {
    if (opened && !data) {
      setOpened(false);
      openModal({
        title: getTranslatedText(titleError, lang),
        children: (
          <p style={{ textAlign: 'center' }}>
            {getTranslatedText(typeError, lang)}
          </p>
        ),
      });
    }
  }, [data, opened, lang, openModal, typeError]);

  const refClickAway = useRef();
  const refHeightCategory = useRef();

  const spring = useSpring({
    opacity: !opened ? 1 : 0,
    scale: !opened ? 1 : 0,
    transformOrigin: !opened ? 5 : 0,
  });

  const dropDown = useTransition(opened, {
    from: { height: 0 },
    enter: { height: refHeightCategory?.current?.offsetHeight },
    leave: { height: 0 },
  });

  const transition = useTransition(opened ? data : [], {
    trail: 75,
    from: { opacity: 0, scale: 0 },
    enter: { opacity: 1, scale: 1, transformOrigin: 5 },
    leave: { opacity: 0, scale: 0 },
    config: { mass: 1, tension: 120, friction: 14 },
  });

  const handleClick = (item) => {
    onClick(item);
    setOpened((prev) => !prev);
  };

  useOutsideClick(refClickAway, () => {
    setOpened(false);
  });

  return (
    <WrapSelect ref={refClickAway}>
      <SelectLabel showImg={showImg} onClick={() => setOpened((prev) => !prev)}>
        {value ? (
          <AnimatedLabel style={spring}>
            {showImg && <Image src={categoryImages[value]} alt={value} />}
            <SelectTitle>{text}</SelectTitle>
          </AnimatedLabel>
        ) : (
          <PlaceHolder>{placeholder}</PlaceHolder>
        )}
      </SelectLabel>
      {dropDown(
        (styles, item) =>
          item && (
            <animated.div style={{ ...styles }}>
              <DropItems
                overflows={overflows}
                ref={refHeightCategory}
                notOpen={opened && !data}
              >
                {transition((styles, item) => (
                  <animated.div key={item} style={{ ...styles }}>
                    <SelectItem
                      showImg={showImg}
                      onClick={() => handleClick(item)}
                      selectedItem={value === item?.name ?? item}
                    >
                      {showImg && (
                        <Image
                          src={categoryImages[item.name]}
                          alt={item.name}
                        />
                      )}
                      <SelectTitle>
                        {categories
                          ? getTranslatedText(`categories.${item?.name}`, lang)
                          : item}
                      </SelectTitle>
                    </SelectItem>
                  </animated.div>
                ))}
              </DropItems>
            </animated.div>
          )
      )}
    </WrapSelect>
  );
};

export { ShowSelectItem };
